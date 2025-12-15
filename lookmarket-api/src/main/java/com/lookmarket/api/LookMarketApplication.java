package com.lookmarket.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * LookMarket - 패션 통합 커머스 플랫폼
 *
 * Java 21 + Spring Boot 3.3.x
 * - Virtual Threads 활성화
 * - Hexagonal Architecture
 * - Event-Driven Architecture (Kafka)
 * - DDD (Domain-Driven Design)
 */
@SpringBootApplication(scanBasePackages = "com.lookmarket")
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class LookMarketApplication {

    public static void main(String[] args) {
        // Java 21 Virtual Threads 활성화
        System.setProperty("spring.threads.virtual.enabled", "true");

        SpringApplication.run(LookMarketApplication.class, args);
    }
}
