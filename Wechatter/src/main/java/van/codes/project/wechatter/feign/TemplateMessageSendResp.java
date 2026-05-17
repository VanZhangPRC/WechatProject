package van.codes.project.wechatter.feign;

import lombok.Data;

@Data
public class TemplateMessageSendResp {
    private Long msgid;
    private Long errcode;
    private String errmsg;
}