package van.codes.project.wechatter.feign;

import lombok.Data;

import java.util.Map;

@Data
public class TemplateMessageSendReq {
    private String touser;
    private String template_id;
    private Map<String, DataElement> data;

    @Data
    public static class DataElement {
        private Object value;

        public DataElement(Object value) {
            this.value = value;
        }
    }
}