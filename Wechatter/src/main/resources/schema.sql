CREATE TABLE IF NOT EXISTS reminder (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id       VARCHAR(64)   NOT NULL COMMENT '用户 openId',
    content       VARCHAR(1024)  NOT NULL COMMENT '通知内容',
    reminder_type VARCHAR(30)   NOT NULL COMMENT '提醒类型',
    target_date   DATE          NULL COMMENT '指定日期使用',
    day_of_month    INT       NULL COMMENT '每月几号',
    last_day_of_month    INT       NULL COMMENT '每月倒数几天',
    day_of_week    VARCHAR(10)       NULL COMMENT '每周周几',
    target_time   TIME          NOT NULL COMMENT '通知时分',
    ai_assisted     BOOL    NOT NULL COMMENT '是否需要AI辅助',
    status         VARCHAR(20)   NOT NULL DEFAULT '启用' COMMENT '状态：启用/已完成/已取消',
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);