package van.project.wechatter.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

        // 1. 解码
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);

        // 2. GZIP解压缩
        String markdown;
        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(decodedBytes))) {
            markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("GZIP解压缩markdown内容失败", e);
            throw new RuntimeException(e);
        }

        // 3. Markdown 转 HTML
        log.debug("markdown: {}", markdown);
        Node document = parser.parse(markdown);

        String htmlBody = renderer.render(document);
        log.debug("htmlBody: {}", htmlBody);
        return htmlBody;
    }

    public static String encode(String content) {
        if (content == null || content.isEmpty())
            return "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("编码markdown内容错误", e);
            throw new RuntimeException(e);
        }
        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(out.toByteArray());
    }
}
