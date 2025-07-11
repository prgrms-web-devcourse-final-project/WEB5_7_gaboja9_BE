package io.gaboja9.mockstock.domain.payments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "결제 요청")
public class PaymentRequest {

    @Schema(description = "충전 금액", example = "10000", required = true)
    @NotBlank
    private int chargeAmount;
}
