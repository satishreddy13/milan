package io.milan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MilanApplication {
    public static void main(String[] args) {
        SpringApplication.run(MilanApplication.class, args);
    }
}
