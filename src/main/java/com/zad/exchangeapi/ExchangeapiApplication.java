package com.zad.exchangeapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExchangeapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeapiApplication.class, args);
    }

}
