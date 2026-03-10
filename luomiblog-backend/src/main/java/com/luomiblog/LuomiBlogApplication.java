package com.luomiblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LuomiBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(LuomiBlogApplication.class, args);
    }
}
