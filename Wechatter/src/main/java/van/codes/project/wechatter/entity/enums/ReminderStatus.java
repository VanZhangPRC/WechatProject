package van.codes.project.wechatter.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReminderStatus {
    ACTIVE("启用"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    @EnumValue
    private final String value;

    ReminderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReminderStatus from(String value) {
        for (ReminderStatus s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ReminderStatus: " + value);
    }
}
