package me.project.joonggoconsumer;

import me.project.joonggocore.CoreConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(CoreConfiguration.class)
@SpringBootApplication
public class JoonggoConsumerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "application-core, application-consumer"); //application이 특정 구성 파일을 추가하도록 설정
        SpringApplication.run(JoonggoConsumerApplication.class, args);
    }

}
