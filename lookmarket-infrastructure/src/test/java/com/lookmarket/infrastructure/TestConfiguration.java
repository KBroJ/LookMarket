package com.lookmarket.infrastructure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 통합 테스트용 Spring Boot 설정 클래스
 *
 * Infrastructure 모듈의 통합 테스트를 위한 최소한의 Spring 컨텍스트를 구성합니다.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.lookmarket.infrastructure")
@EnableJpaRepositories(basePackages = "com.lookmarket.infrastructure")
public class TestConfiguration {
}
