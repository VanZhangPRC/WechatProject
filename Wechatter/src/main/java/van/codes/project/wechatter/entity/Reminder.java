package van.codes.project.wechatter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import van.codes.project.wechatter.entity.enums.ReminderStatus;
import van.codes.project.wechatter.entity.enums.ReminderType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("reminder")
public class Reminder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 openId */
    private String openId;

    /** 通知内容 */
    private String content;

    /** 提醒类型 */
    private ReminderType reminderType;

    /** 类型1使用：指定日期（如 2025-06-01） */
    private LocalDate targetDate;

    /** 类型2使用：每月几号(1-31)；类型3使用：每月倒数几天(1-31) */
    private Integer targetDay;

    /** 所有类型通用：通知时分 */
    private LocalTime targetTime;

    /** 重复次数：NULL或0=一直持续，N=重复N次 */
    private Integer repeatCount;

    /** 已执行次数 */
    private Integer executedCount;

    /** 状态 */
    private ReminderStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}