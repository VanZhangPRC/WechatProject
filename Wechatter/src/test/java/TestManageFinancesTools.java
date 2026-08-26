import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import van.project.wechatter.WechatterApplication;
import van.project.wechatter.aitool.ManageFinancesTools;

import java.math.BigDecimal;
import java.util.Map;

@SpringBootTest(classes = WechatterApplication.class,  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestManageFinancesTools {

    @Autowired
    private ManageFinancesTools manageFinancesTools;

    @Test
    @DisplayName("测试获取黄金价格")
    public void testGoldenPrice() {
        Map<String, BigDecimal> goldenPrice = manageFinancesTools.goldenPrice();
        Assertions.assertNotNull(goldenPrice);
        System.out.println(goldenPrice);
    }

    @Test
    @DisplayName("测试获取美元汇率")
    public void testDollarPrice() {
        BigDecimal dollarExchange = manageFinancesTools.dollarExchange();
        Assertions.assertNotNull(dollarExchange);
        System.out.println(dollarExchange);
    }

}
