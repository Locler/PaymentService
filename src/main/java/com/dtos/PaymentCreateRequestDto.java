package com.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequestDto {

    @NotNull
    @Positive
    private Long orderId;

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @DecimalMin(value = "0.01",message = "Payment amount must be more then 0")
    private BigDecimal paymentAmount;
}
