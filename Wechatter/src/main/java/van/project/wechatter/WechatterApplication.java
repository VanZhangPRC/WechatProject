package van.project.wechatter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("van.project.wechatter.mapper")
@EnableScheduling
public class WechatterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatterApplication.class, args);
    }
}
