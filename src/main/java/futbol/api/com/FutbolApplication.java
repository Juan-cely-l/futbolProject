package futbol.api.com;

import futbol.api.com.external.config.FootballApiConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(FootballApiConfig.class)
@EnableAsync
public class FutbolApplication{
    public static void main(String[] args){
        SpringApplication.run(FutbolApplication.class,args);
    }
}