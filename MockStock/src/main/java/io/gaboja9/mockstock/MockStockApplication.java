package io.gaboja9.mockstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MockStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockStockApplication.class, args);
    }
}
