import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import van.project.wechatter.WechatterApplication;
import van.project.wechatter.util.MarkdownUtil;
import van.project.wechat.wechatPublic.services.WechatApiExecutor;
import van.project.wechat.wechatPublic.services.api.TemplateMessageSendReq;
import van.project.wechat.wechatPublic.services.api.TemplateMessageSendResp;

import java.io.IOException;
import java.util.Collections;

@SpringBootTest(classes = WechatterApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestWechatAPI {

    private final String OPEN_ID = "oCdDY14-mL0Xab0KpADxtleiW44E";
    private final String TEMPLATE_ID = "bnDl2Gnssulz21Opa1zPawCJi6S5WDF91k6F8E-DZKE";

    @Autowired
    private WechatApiExecutor apiExecutor;

    @Test
    @DisplayName("测试模版消息发送")
    public void testTemplateMessage() {
        TemplateMessageSendResp result = apiExecutor.sendTemplateMessage(OPEN_ID, TEMPLATE_ID, Collections.singletonMap("content", new TemplateMessageSendReq.DataElement("hello world..")));
        Assertions.assertEquals(0, result.getErrcode());
    }

    @Test
    @DisplayName("测试Markdown接口作为模版消息预览预览")
    public void test() throws IOException {

        String content = "\uD83D\uDCCA 上海黄金交易所最新行情\n" +
                "\n" +
                "| 项目 | 价格（元/克） |\n" +
                "|------|------------|\n" +
                "| 开盘价 | 990.0 |\n" +
                "| 最低价 | 985.0 |\n" +
                "| 最新价 | 1003.9 |\n" +
                "| 最高价 | 1006.6 |\n" +
                "\n" +
                "\uD83D\uDCB1 美元汇率\n" +
                "- 100美元 = **672.91元**人民币\n" +
                "\n" +
                "如需定时查看金价/汇率，随时告诉我哦～";

        String encodedContent = MarkdownUtil.encode(content);
        TemplateMessageSendResp resp = apiExecutor.sendTemplateMessage(
                TemplateMessageSendReq.builder()
                        .template_id(TEMPLATE_ID)
                        .touser(OPEN_ID)
                        .url("./markdown/view?data=" + encodedContent)
                        .data(Collections.singletonMap("content", new  TemplateMessageSendReq.DataElement("hello world..")))
                        .build());
        Assertions.assertEquals(0, resp.getErrcode());
        System.in.read();
    }
}
