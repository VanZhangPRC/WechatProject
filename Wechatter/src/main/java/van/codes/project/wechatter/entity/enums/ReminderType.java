package van.codes.project.wechatter.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReminderType {
    SPECIFIC_DATE("指定日期"),
    MONTHLY_DAY("每月几号"),
    MONTHLY_LAST_N_DAYS("每月倒数几天");

    @EnumValue
    private final String value;

    ReminderType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReminderType from(String value) {
        for (ReminderType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown ReminderType: " + value);
    }
}
