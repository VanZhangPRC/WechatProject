package van.codes.project.wechatter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients(basePackages = "van.codes.project.wechatter.feign")
@MapperScan("van.codes.project.wechatter.mapper")
@EnableScheduling
public class WechatterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatterApplication.class, args);
    }
}
