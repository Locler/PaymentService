package com.dtos;

import com.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private Long id;

    @NotNull
    private Long orderId;

    @NotNull
    private Long userId;

    @NotBlank
    private PaymentStatus status;

    @NotNull
    @DecimalMin(value = "0.01",message = "Payment amount must be more then 0")
    private BigDecimal paymentAmount;

}
