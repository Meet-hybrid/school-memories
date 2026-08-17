package com.keepsake.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KeepsakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeepsakeApplication.class, args);
    }
}
