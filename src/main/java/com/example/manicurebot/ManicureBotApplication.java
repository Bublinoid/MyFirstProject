package com.example.manicurebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan({"com.example.manicurebot.Appointment", "com.example.manicurebot.Feedback", "com.example.manicurebot.Gcp", "com.example.manicurebot.Telegram", "com.example.manicurebot.User"})
public class ManicureBotApplication {

    public static void main(String[] args) {
        String port = System.getenv("PORT");

        if (port == null || port.isEmpty()) {
            System.out.println("Port not set in the environment. Using default port 8080.");
            port = "8080";
        }

        System.setProperty("server.port", port);
        SpringApplication.run(ManicureBotApplication.class, args);
    }
}
