package com.tripagora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TripAgoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripAgoraApplication.class, args);
    }
}