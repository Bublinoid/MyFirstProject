package com.example.manicurebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.example.manicurebot")
public class ManicureBotApplication {

    public static void main(String[] args) {
        System.out.println("hello world");
        SpringApplication.run(ManicureBotApplication.class, args);
    }
}
