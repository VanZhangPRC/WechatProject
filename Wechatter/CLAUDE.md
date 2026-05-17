# Wechatter

微信公众号后台。

## 技术栈

- **Java 17** · **Spring Boot 3.4.6** · **Maven**（本地需安装 Maven）
- **Spring AI 1.1.6**：OpenAI starter（对接 DeepSeek API）+ MCP Server WebMVC（暴露提醒工具）
- **MyBatis-Plus 3.5.9** + **H2** 嵌入式数据库（文件模式：`data/wechatter.mv.db`）
- **OpenFeign 4.2.1**：调用微信服务端 API（access_token、模版消息）
- **Lombok**

## 构建与运行

```bash
# 开发运行（从 Wechatter 目录）
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --wechat.app-id=<AppID> \
  --wechat.secret=<AppSecret> \
  --wechat.token=<Token> \
  --wechat.template-id=<模版ID>"

# 仅打包
mvn package -DskipTests

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=WeChatApiServiceTest
```

AI 服务使用 DeepSeek API，环境变量 `DEEPSEEK_API_KEY` 可覆盖 application.yml 中的默认 key。

## 项目结构

```
src/main/java/van/codes/project/wechatter/
├── WechatterApplication.java         入口（启用 Feign、MapperScan、Scheduling）
├── controller/
│   └── WeChatController.java         GET /wechat（验证）、POST /wechat（接收消息）
├── config/
│   ├── WeChatProperties.java         @ConfigurationProperties(prefix="wechat")
│   └── ToolConfig.java               ChatMemory + ToolCallbackProvider 注册
├── message/
│   ├── MessageHandler.java           消息处理策略接口（getMsgType + handle）
│   ├── MessageDispatcher.java        根据 MsgType 路由到对应 Handler
│   └── handler/
│       ├── TextMessageHandler.java   文本 → AI 处理 → XML 回复
│       ├── ImageMessageHandler.java  图片消息
│       ├── VoiceMessageHandler.java  语音消息
│       ├── VideoMessageHandler.java  视频消息
│       ├── ShortVideoMessageHandler.java 短视频消息
│       ├── LocationMessageHandler.java   位置消息
│       ├── LinkMessageHandler.java       链接消息
│       └── EventMessageHandler.java      事件消息（关注/取消关注等）
├── service/
│   ├── WeChatMessageAiService.java   AI ChatClient 调用（提醒助手 System Prompt）
│   ├── WeChatApiService.java         微信 API：发送模版消息
│   ├── WeChatTokenManager.java       access_token 生命周期管理（获取、缓存、刷新）
│   ├── ReminderService.java          提醒 CRUD
│   └── ReminderSchedulerService.java 提醒调度引擎
├── tool/
│   └── ReminderTools.java            @Tool 工具集：add/list/cancelReminder + getCurrentDateTime
├── entity/
│   ├── Reminder.java                 提醒实体
│   └── enums/
│       ├── ReminderType.java         SPECIFIC_DATE / MONTHLY_DAY / MONTHLY_LAST_N_DAYS
│       └── ReminderStatus.java       ACTIVE / COMPLETED / CANCELLED
├── mapper/
│   └── ReminderMapper.java           MyBatis-Plus BaseMapper
├── feign/
│   ├── WechatFeign.java              Feign 接口（access_token + template_message）
│   ├── AccessTokenReq/Resp.java
│   └── TemplateMessageSendReq/Resp.java
└── util/
    ├── XmlUtil.java                  微信 XML 消息体 ⇄ Map<String,String> + 构建文本回复 XML
    └── MessageDeduplicator.java      消息去重（MsgId，纯内存 ConcurrentHashMap）
```

## 数据库

H2 嵌入式数据库，文件存储在 `data/` 目录。表结构由 `src/main/resources/schema.sql` 自动初始化。

H2 控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:file:./data/wechatter`，用户名 sa，无密码）

### reminder 表

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK AUTO | |
| open_id | VARCHAR(64) | 微信用户 openId |
| content | VARCHAR(500) | 提醒内容 |
| reminder_type | VARCHAR(20) | SPECIFIC_DATE / MONTHLY_DAY / MONTHLY_LAST_N_DAYS |
| target_date | DATE | 指定日期型使用 |
| target_day | TINYINT | 每月几号 / 每月倒数几天 |
| target_time | TIME | 通知时分 |
| repeat_count | INT | NULL/0=一直持续，N=重复N次 |
| executed_count | INT | 已执行次数 |
| status | VARCHAR(20) | ACTIVE / COMPLETED / CANCELLED |
| create_time | TIMESTAMP | |
| update_time | TIMESTAMP | |

## 核心流程

### 1. 微信消息接入

```
微信 POST XML → WeChatController.receive()
  → XmlUtil.parse() 解析为 Map
  → MessageDeduplicator.tryAcquire(MsgId)
    ├─ null（首次）→ MessageDispatcher.dispatch() → Handler.handle() → XML
    │   → MessageDeduplicator.complete(msgId, result)
    └─ Future（重试）→ 等待首次结果，返回相同内容
```

微信对未及时响应的消息最多重试 3 次（间隔约 5s），去重器 60s 窗口内同 MsgId 返回空串。

### 2. AI 对话（文本消息）

```
TextMessageHandler.handle()
  → WeChatMessageAiService.processMessage(openId, content)
    → ChatClient.prompt()
      .defaultSystem("你是提醒助手...")
      .defaultToolCallbacks(reminderTools)
      .defaultAdvisors(MessageChatMemory → 窗口 10 条，按 openId 隔离)
      .user(text + openId)
      .call()
    → 返回 AI 文本
  → XmlUtil.buildTextReply() 构建回复 XML
```

AI 模型：DeepSeek V4 Flash（通过 OpenAI 兼容接口）。

### 3. 提醒调度引擎

```
启动：@PostConstruct init() → 加载 ACTIVE 提醒 → scheduleOrCancel()

新增：ReminderService.add() → 入库 → schedulerService.schedule()
  → calcNextTrigger() → TaskScheduler.schedule(execute, instant)

触发：execute(id)
  → 查库（防并发状态变更）
  → WeChatApiService.sendReminder() 推模版消息
  → executedCount++，判断是否完成
  → 计算下次触发 → 重新 schedule

取消：ReminderService.cancel()
  → DB status=CANCELLED → cancelScheduled() 移除任务
```

三种提醒类型的触发时间计算：
- **指定日期**：targetDate + executedCount × 1 天
- **每月几号**：当月 targetDay 号 + executedCount × 1 月（自动处理月末边界）
- **每月倒数几天**：当月最后一天 - targetDay + 1 + executedCount × 1 月

### 4. MCP Server

Spring AI MCP Server WebMVC 将 `ReminderTools` 暴露为 MCP 协议端点，第三方 MCP 客户端可直接调用。

## 配置

`src/main/resources/application.yml` 关键项：

| 配置 | 说明 |
|------|------|
| `spring.ai.openai.base-url` | https://api.deepseek.com |
| `spring.ai.openai.chat.options.model` | deepseek-v4-flash |
| `wechat.app-id` | 公众号 AppID |
| `wechat.secret` | 公众号 AppSecret |
| `wechat.token` | 服务器验证 Token |
| `wechat.template-id` | 模版消息 ID |
| `spring.datasource.url` | jdbc:h2:file:./data/wechatter |
