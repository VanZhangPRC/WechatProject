package van.project.wechatter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.EnumTypeHandler;
import van.project.wechatter.entity.enums.ReminderStatus;
import van.project.wechatter.entity.enums.ReminderType;

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
    @TableField(typeHandler = EnumTypeHandler.class)
    private ReminderType reminderType;

    /** 指定日期类型使用：指定日期（如 2025-06-01） */
    private LocalDate targetDate;

    /** 每月几号类型使用：每月几号(1-31)；*/
    private Integer dayOfMonth;

    /** 每月倒数几天类型使用：每月倒数几天； */
    private Integer lastDayOfMonth;

    /** 每周执行类型使用：每周第几天，(1-Monday周一，2-Tuesday周二，3-Wednesday周三，4-Thursday周四，5-Friday周五，6-Saturday周六，7-Sunday周日) */
    private String dayOfWeek;

    /** 所有类型通用：通知时分 */
    private LocalTime targetTime;

    private boolean aiAssisted = false;

    /** 状态 */
    private ReminderStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}