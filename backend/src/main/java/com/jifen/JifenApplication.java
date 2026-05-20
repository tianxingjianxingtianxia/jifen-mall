package com.jifen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JifenApplication {

    public static void main(String[] args) {
        SpringApplication.run(JifenApplication.class, args);
    }
}
