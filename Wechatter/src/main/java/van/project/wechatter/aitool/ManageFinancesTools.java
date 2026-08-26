package van.project.wechatter.aitool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ManageFinancesTools {

    @Value("${APIHZ_ID:}")
    private String apihz_id;
    @Value("${APIHZ_KEY:}")
    private String apihz_key;

    private final RestTemplate restTemplate;

    private final String APIHZ_HOST = "https://cn.apihz.cn/api";
    private final String GOLDEN_PRICE = "/jinrong/goldshnew.php";
    private final String DOLLAR_EXCHANGE = "/jinrong/huilv.php";

    public ManageFinancesTools(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.build();
    }

    /**
     * 查询当前黄金价格
     * @return 1g黄金当前人民币价格
     */
    @Tool(name = "goldenPrice", description = "查询上海黄金交易所最新金价，请求出错时返回 null，成功时返回最低价、最新价、最高价、开盘价")
    public Map<String, BigDecimal> goldenPrice() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(RequestEntity.get(APIHZ_HOST + GOLDEN_PRICE + "?id={1}&key={2}", apihz_id, apihz_key).build(), JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("获取黄金价格失败：{}: {}", response.getStatusCode(), response);
            return null;
        }

        JsonNode body = response.getBody();
        if (body.get("code").asInt() != 200) {
            log.error("获取黄金价格失败: {}", response);
            return null;
        }

        ArrayNode data = body.withArrayProperty("data");
        for (JsonNode node : data) {
            if ("Au99.99".equals(node.findValue("合约").asText())) {
                Map<String, BigDecimal> result = new HashMap<>();
                result.put("最新价", new BigDecimal(node.findValue("最新价").asText()));
                result.put("最高价", new BigDecimal(node.findValue("最高价").asText()));
                result.put("最低价", new BigDecimal(node.findValue("最低价").asText()));
                result.put("开盘价", new BigDecimal(node.findValue("开盘价").asText()));
                return result;
            }
        }
        log.error("获取不到 合约Au99.99 的金价，请确认报文结构");
        return null;
    }

    @Tool(name = "dollarExchange", description = "查询美元汇率信息，返回100美元兑换人民币价格，请求出错时返回 null")
    public BigDecimal dollarExchange() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                RequestEntity.get(APIHZ_HOST + DOLLAR_EXCHANGE + "?id={1}&key={2}&from={3}&to={4}&money={5}",
                        apihz_id, apihz_key, "USD", "CNY", 100).build(),
                JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("获取美元汇率失败：{}: {}", response.getStatusCode(), response);
            return null;
        }

        JsonNode body = response.getBody();
        if (body.get("code").asInt() != 200) {
            log.error("获取美元汇率失败: {}", response);
            return null;
        }

        try {
            return new BigDecimal(body.get("result").asText());
        } catch (Exception e) {
            log.error("获取美元汇率失败", e);
            return null;
        }
    }
}
