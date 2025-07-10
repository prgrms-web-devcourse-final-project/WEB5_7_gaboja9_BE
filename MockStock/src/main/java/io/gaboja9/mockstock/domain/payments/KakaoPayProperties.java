package io.gaboja9.mockstock.domain.payments;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakaopay")
@Data
public class KakaoPayProperties {
    private String secretKey;
    private String readyUrl;
    private String approveUrl;
    private String cancelUrl;
    private String cid;
}
