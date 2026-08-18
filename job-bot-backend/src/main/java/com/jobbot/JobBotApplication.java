package com.jobbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobBotApplication.class, args);
    }
}

