package me.project.joonggoapi;

import me.project.joonggocore.CoreConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(CoreConfiguration.class)
@SpringBootApplication
public class JoonggoApiApplication {

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "application-core, application-api"); //application이 특정 구성 파일을 추가하도록 설정
        SpringApplication.run(JoonggoApiApplication.class, args);
    }

}
