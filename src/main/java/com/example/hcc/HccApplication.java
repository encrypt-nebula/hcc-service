package com.example.hcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ComponentScan(basePackages = {"com.medryte", "com.example.hcc"})
@EnableJpaAuditing
public class HccApplication {

    public static void main(String[] args) {
        System.out.println("Run Main Class");
        SpringApplication.run(HccApplication.class, args);
    }
}
