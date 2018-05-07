package com.even.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by davi on 17-4-29.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.even"})
public class Application{
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
