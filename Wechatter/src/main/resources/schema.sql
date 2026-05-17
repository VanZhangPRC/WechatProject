CREATE TABLE IF NOT EXISTS reminder (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id       VARCHAR(64)   NOT NULL COMMENT '用户 openId',
    content       VARCHAR(500)  NOT NULL COMMENT '通知内容',
    reminder_type VARCHAR(20)   NOT NULL COMMENT '提醒类型',
    target_date   DATE          NULL COMMENT '指定日期使用',
    target_day    TINYINT       NULL COMMENT '每月几号或倒数几天',
    target_time   TIME          NOT NULL COMMENT '通知时分',
    repeat_count   INT           NULL COMMENT '重复次数：NULL/0=一直持续',
    executed_count INT           NOT NULL DEFAULT 0 COMMENT '已执行次数',
    status         VARCHAR(20)   NOT NULL DEFAULT '启用' COMMENT '状态：启用/已完成/已取消',
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);