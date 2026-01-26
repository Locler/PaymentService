package com.mapper;

import com.dtos.PaymentCreateRequestDto;
import com.dtos.PaymentResponseDto;
import com.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toEntity(PaymentCreateRequestDto dto);

    PaymentResponseDto toDto(Payment entity);
}
