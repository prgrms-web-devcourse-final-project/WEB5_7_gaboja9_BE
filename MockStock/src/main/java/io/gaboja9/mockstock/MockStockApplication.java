package io.gaboja9.mockstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class MockStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockStockApplication.class, args);
    }
}
