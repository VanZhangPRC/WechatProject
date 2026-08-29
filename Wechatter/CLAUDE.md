# Wechatter

微信公众号后台，带AI辅助的个人助手，个人开发的小玩具

## 技术栈

- **Java 17** + **Spring Boot 3.5.9**，Maven 构建（打包名 `wechatter.jar`）
- **Spring AI 1.1.8**（BOM 管理）：
    - `spring-ai-starter-model-deepseek`：DeepSeek 对话模型（`deepseek-v4-flash`），ChatClient + 窗口会话记忆（MessageWindowChatMemory，20 条）
    - `spring-ai-starter-mcp-client`：MCP 客户端；MCP Server 能力已停用（pom 中注释），工具改为本地 `MethodToolCallbackProvider` 注册
- **WechatPublic**（`com.github.VanZhangPRC:WechatPublic:main-SNAPSHOT`，JitPack 仓库）：微信公众号接入框架，提供消息回调接口（`IMessageReceiver`）、API 执行器（`WechatApiExecutor`）、模板消息发送
- **MyBatis-Plus 3.5.9**（spring-boot3-starter）：`BaseMapper` + `LambdaQueryWrapper`，下划线 ↔ 驼峰自动映射
- **H2** 嵌入式数据库（文件模式）
- **CommonMark 0.22.0**（含 GFM Tables 扩展）：Markdown → HTML 渲染
- **Thymeleaf**：提醒详情落地页模板
- **Lombok**：`@Slf4j` / `@RequiredArgsConstructor` / `@Data` / `@Builder`
- 测试：`spring-boot-starter-test`（JUnit 5）

## 构建与运行

```bash
# 开发运行（从 Wechatter 目录）
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --wechat.app-id=<AppID> \
  --wechat.secret=<AppSecret> \
  --wechat.token=<Token>"

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
Wechatter/
├── pom.xml                                  # Maven 配置，打包名 wechatter.jar
└── src/
    ├── main/
    │   ├── java/van/project/wechatter/      # 根包：启动类（@MapperScan + @EnableScheduling）
    │   │   ├── wechat/                      # 微信消息接入：消息分发（MessageReceiver）、AI 对话（AIChatterService）
    │   │   ├── aitool/                      # AI 本地工具（@Tool 注册）：提醒、时间、金融查询、联网搜索
    │   │   ├── config/                      # Spring 配置：ChatClient、会话记忆、工具注册
    │   │   ├── controller/                  # Web 控制器：提醒详情 Markdown 落地页
    │   │   ├── entity/                      # MyBatis-Plus 实体及枚举（Reminder / ReminderStatus / ReminderType）
    │   │   ├── mapper/                      # MyBatis-Plus Mapper 接口（BaseMapper）
    │   │   ├── service/                     # 业务层：提醒 CRUD、定时调度与模板消息推送
    │   │   └── util/                        # 工具类：Markdown 渲染、微信 XML 解析
    │   └── resources/
    │       ├── mapper/                      # MyBatis XML 映射文件（暂为空）
    │       └── templates/                   # Thymeleaf 模板（提醒详情落地页）
    └── test/
        ├── java/                            # 测试类（平铺，无包结构）
        └── resources/                       # 测试配置（application-test.yml）
```

> `data/`（H2 数据文件）、`logs/`（运行日志）、`target/`（构建产物）、`.idea/` 等目录已省略。

## 数据库

H2 嵌入式数据库，文件存储在 `data/` 目录。表结构由 `src/main/resources/schema.sql` 自动初始化。

H2 控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:file:./data/wechatter`，用户名 sa，无密码）


## 核心流程

### 1. 微信消息接入

微信对未及时响应的消息最多重试 3 次（间隔约 5s），去重器 60s 窗口内同 MsgId 返回空串。

### 2. AI 对话（文本消息）

AI 模型：DeepSeek V4 Flash（通过 OpenAI 兼容接口）。
