import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import van.project.wechatter.WechatterApplication;
import van.project.wechatter.aitool.ManageFinancesTools;
import van.project.wechatter.aitool.SearchTool;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = WechatterApplication.class,  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestToolCalling {

    @Autowired
    private ManageFinancesTools manageFinancesTools;
    @Autowired
    private SearchTool searchTool;
    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("测试获取 TollProvider")
    public void testToolProvider() {
        Map<String, ToolCallbackProvider> beans = context.getBeansOfType(ToolCallbackProvider.class);
        assertNotNull(beans);
        System.out.println(beans);
    }

    @Test
    @DisplayName("测试获取黄金价格")
    public void testGoldenPrice() {
        Map<String, BigDecimal> goldenPrice = manageFinancesTools.goldenPrice();
        assertNotNull(goldenPrice);
        System.out.println(goldenPrice);
    }

    @Test
    @DisplayName("测试获取美元汇率")
    public void testDollarPrice() {
        BigDecimal dollarExchange = manageFinancesTools.dollarExchange();
        assertNotNull(dollarExchange);
        System.out.println(dollarExchange);
    }

    @Test
    @DisplayName("测试搜索功能")
    public void testWebSearch() {
        SearchTool.WebSearchResponse news = searchTool.webSearch("今日十大新闻");
        assertNotNull(news);
        assertNotNull(news.getResults());
        System.out.println(news.getResults());
    }

}
