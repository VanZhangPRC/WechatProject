package van.project.wechatter.entity.enums;

public enum ReminderType {
    SPECIFIC_DATE("指定日期"),
    MONTHLY_DAY("每月几号"),
    MONTHLY_LAST_N_DAYS("每月倒数几天"),
    EVERY_DAY("每天执行"),
    EVERY_WEEK("每周执行");

    private final String value;

    ReminderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReminderType from(String value) {
        for (ReminderType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown ReminderType: " + value);
    }
}
