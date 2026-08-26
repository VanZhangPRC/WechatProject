package van.project.wechatter.aitool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class CommonTools {

    @Tool(name = "getCurrentDateTime", description = "查询当前日期时间，需要获取当前日期时间时使用")
    public LocalDateTime getCurrentDate() {
        return LocalDateTime.now();
    }

}
