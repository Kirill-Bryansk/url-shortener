package ru.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LinkServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LinkServiceApplication.class, args);
    }
}
