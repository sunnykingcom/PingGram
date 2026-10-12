package com.pinggram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableAsync
public class PingGramApplication {
    public static void main(String[] args) {
        SpringApplication.run(PingGramApplication.class, args);
    }
}
