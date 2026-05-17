package van.codes.project.wechatter.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class XmlUtil {

    private XmlUtil() {}

    /** 构建微信被动文本回复 XML */
    public static String buildTextReply(String toUser, String fromUser, String content) {
        return String.format("""
                <xml>
                <ToUserName><![CDATA[%s]]></ToUserName>
                <FromUserName><![CDATA[%s]]></FromUserName>
                <CreateTime>%d</CreateTime>
                <MsgType><![CDATA[text]]></MsgType>
                <Content><![CDATA[%s]]></Content>
                </xml>""", toUser, fromUser, System.currentTimeMillis() / 1000, content);
    }

    /** 将微信 XML 消息解析为 Map，key 为标签名，value 为文本内容 */
    public static Map<String, String> parse(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();

            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element element) {
                    map.put(element.getTagName(), element.getTextContent());
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML", e);
        }
    }
}
