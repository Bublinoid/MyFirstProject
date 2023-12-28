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
        // Чтение порта из переменной окружения
        String port = System.getenv("PORT");

        // Проверка, был ли установлен порт в переменной окружения
        if (port == null || port.isEmpty()) {
            System.out.println("Port not set in the environment. Using default port 8080.");
            port = "8080";
        }

        // Установка порта в системное свойство Spring Boot
        System.setProperty("server.port", port);

        // Запуск приложения Spring Boot
        SpringApplication.run(ManicureBotApplication.class, args);
    }
}
