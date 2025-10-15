package com.example.opensearchttl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"service","config","controller"})
public class OpenSearchTtlApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenSearchTtlApplication.class, args);
    }

}
