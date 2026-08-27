package van.project.wechatter.aitool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 联网搜索工具：调用 SearXNG 聚合搜索引擎，为 AI 提供实时联网检索能力。
 * <p>SearXNG 以 JSON 格式对外提供搜索 API，使用前需在其 settings.yml 的
 * {@code search.formats} 中开启 json 格式，否则接口返回 403。</p>
 */
@Component
@Slf4j
public class SearchTool {

    /** 返回给 AI 的最大条数，控制单次结果的 token 开销 */
    private static final int MAX_RESULTS = 10;

    /** SearXNG 服务地址，默认本地 9999 端口 */
    @Value("${wechatter.searxng.host:http://localhost:9999}")
    private String host;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SearchTool(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        // 搜索发生在 AI 对话链路中，必须限制超时，避免个别引擎响应慢拖垮整轮回复
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 SearXNG 搜索，结果按相关度得分降序排列并截取前 {@value MAX_RESULTS} 条，
     * 外层保留查询词、直接答案、关键词纠错与搜索建议等辅助信息
     *
     * @param query 搜索关键词，用简洁的自然语言或关键词组合描述要查的内容
     * @return 搜索响应；请求出错时返回 null
     */
    @Tool(name = "webSearch", description = "联网搜索工具：查询互联网上的公开信息，适用于解答时效性强的问题"
            + "（如最新新闻、天气、行情、软件版本等），需要用户提供事实依据时也可使用。请求出错时返回 null。"
            + "返回结构：{\"query\":本次查询词, \"answers\":即时答案列表（元素为纯文本或含 answer 字段的对象），"
            + "\"corrections\":关键词纠错建议, \"suggestions\":相关搜索建议，"
            + "\"results\":[{\"title\":页面标题, \"url\":页面链接, \"content\":内容摘要，"
            + "\"publishedDate\":发布日期或 null, \"engine\":来源引擎}]}。"
            + "results 已按相关度降序排列，最多返回 10 条")
    public WebSearchResponse webSearch(@ToolParam(description = "搜索关键词") String query) {
        if (!StringUtils.hasText(query)) {
            log.warn("联网搜索被跳过：搜索关键词为空");
            return null;
        }

        RequestEntity<Void> request = RequestEntity.get(baseUrl() + "/search?q={1}&format=json", query)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientException e) {
            log.error("联网搜索失败：连接 SearXNG({}) 异常", host, e);
            return null;
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("联网搜索失败：{}: {}", response.getStatusCode(), response.getBody());
            return null;
        }

        String rawBody = response.getBody();
        if (!StringUtils.hasLength(rawBody)) {
            log.error("联网搜索失败：响应体为空");
            return null;
        }

        WebSearchResponse body;
        try {
            body = objectMapper.readValue(rawBody, WebSearchResponse.class);
        } catch (IOException e) {
            log.error("联网搜索失败：解析响应体失败", e);
            return null;
        }

        // SearXNG 返回时已按相关度粗排，这里再按得分精确降序截取前 N 条；
        // 报文缺失 score 时按 0 分处理，稳定排序可保持原有顺序
        if (body.getResults() == null) {
            body.setResults(List.of());
        }
        List<WebSearchResult> topResults = body.getResults().stream()
                .sorted(Comparator.comparingDouble(WebSearchResult::getScore).reversed())
                .limit(MAX_RESULTS)
                .toList();
        body.setResults(topResults);

        if (topResults.isEmpty()) {
            log.info("联网搜索无结果：{}", query);
        }
        return body;
    }

    /**
     * 补全协议头：允许配置值带或不带 http(s) 前缀
     */
    private String baseUrl() {
        return host.startsWith("http") ? host : "http://" + host;
    }


    /**
     * SearXNG 响应外层结构，仅保留对 AI 有价值的辅助信息，
     * 报文中的 infoboxes、unresponsive_engines 等冗余字段自动忽略
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebSearchResponse {

        /** 本次查询词 */
        private String query;

        /** 直接答案（即时回答，如天气、汇率类查询），字符串或 {"answer": ..} 对象结构 */
        private List<Object> answers;

        /** 关键词纠错建议 */
        private List<String> corrections;

        /** 相关搜索建议 */
        private List<String> suggestions;

        /** 网页搜索结果，按相关度降序的前若干条 */
        private List<WebSearchResult> results;
    }

    /**
     * 单条搜索结果，仅保留标题、链接、摘要、发布日期、来源等有价值字段，
     * 过滤掉 SearXNG 原始报文中的评分、模板等冗余元数据，减少传给 AI 的 token 消耗
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebSearchResult {

        /** 页面标题 */
        private String title;

        /** 页面链接 */
        private String url;

        /** 内容摘要 */
        private String content;

        /** 发布日期，可能为 null */
        private String publishedDate;

        /** 结果来源引擎 */
        private String engine;

        /** 相关度评分，仅用于结果排序，不输出给 AI */
        @JsonIgnore
        private double score;
    }
}
