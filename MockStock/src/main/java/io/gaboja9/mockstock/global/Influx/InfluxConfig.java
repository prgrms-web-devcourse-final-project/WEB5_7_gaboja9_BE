package io.gaboja9.mockstock.global.Influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxConfig {

    @Bean
    public InfluxDBClient influxDBClient() {
        String url = "http://localhost:8086";
        String token = "your-token";
        String org = "your-org";
        String bucket = "your-bucket";

        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}