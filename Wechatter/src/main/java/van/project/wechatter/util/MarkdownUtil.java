package van.project.wechatter.util;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
public class MarkdownUtil {

    private static final Parser parser;
    private static final HtmlRenderer renderer;

    static {
        List<Extension> extensions = List.of(TablesExtension.create());
        parser = Parser
                .builder()
                .extensions(extensions)
                .build();
        renderer = HtmlRenderer
                .builder()
                .extensions(extensions)
                .build();
    }

    public static String decodeAndRender(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return "<p>空内容</p>";
        }

        // 1. 补齐 Base64 填充位（URL-safe 去掉了 '='）
        String base64 = encoded
                .replace('-', '+')
                .replace('_', '/');
        int mod = base64.length() % 4;
        if (mod == 2) base64 += "==";
        else if (mod == 3) base64 += "=";
        else if (mod == 1) {
            // 理论上不会出现，但防御处理
            throw new IllegalArgumentException("Invalid Base64 format");
        }

        // 2. 解码
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String markdown = new String(decodedBytes, StandardCharsets.UTF_8);
        log.debug("markdown: {}", markdown);

        // 3. Markdown 转 HTML
        Node document = parser.parse(markdown);

        String htmlBody = renderer.render(document);
        log.debug("htmlBody: {}", htmlBody);
        return htmlBody;
    }

    public static String encode(String content) {
        if (content == null || content.isEmpty())
            return "";

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}
