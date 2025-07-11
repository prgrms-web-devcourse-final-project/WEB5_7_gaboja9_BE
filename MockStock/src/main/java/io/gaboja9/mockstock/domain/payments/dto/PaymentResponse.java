package io.gaboja9.mockstock.domain.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private boolean success;
    private String message;
    private Object data;

    public static PaymentResponse success(String message, Object data) {
        return new PaymentResponse(true, message, data);
    }

    public static PaymentResponse fail(String message) {
        return new PaymentResponse(false, message, null);
    }
}
