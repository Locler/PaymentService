package com.service;

import com.accessChecker.AccessChecker;
import com.client.OrderServiceClient;
import com.client.RandomNumberClient;
import com.client.UserServiceClient;
import com.dtos.OrderDto;
import com.dtos.PaymentCreateRequestDto;
import com.dtos.PaymentResponseDto;
import com.dtos.UserInfoDto;
import com.entity.Payment;
import com.enums.PaymentStatus;
import com.enums.UserRole;
import com.mapper.PaymentMapper;
import com.repository.PaymentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper mapper;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final RandomNumberClient randomNumberClient;
    private final AccessChecker accessChecker;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, PaymentMapper mapper, UserServiceClient userServiceClient, OrderServiceClient orderServiceClient, RandomNumberClient randomNumberClient, AccessChecker accessChecker) {
        this.paymentRepository = paymentRepository;
        this.mapper = mapper;
        this.userServiceClient = userServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.randomNumberClient = randomNumberClient;
        this.accessChecker = accessChecker;
    }

    @Transactional
    public PaymentResponseDto createPayment(
            @Valid @NotNull PaymentCreateRequestDto dto,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        if (dto.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        accessChecker.checkUserAccess(dto.getUserId(), requesterId, roles);

        UserInfoDto user = userServiceClient.getUserById(dto.getUserId(), requesterId, roles);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Inactive or unknown user");
        }

        OrderDto order = orderServiceClient.getOrderById(dto.getOrderId(), requesterId, roles);

        if (Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalStateException("Cannot create payment for deleted order");
        }

        if (!"NEW".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot create payment for order with status " + order.getStatus());
        }

        Payment payment = mapper.toEntity(dto);
        payment.setStatus(randomNumberClient.resolvePaymentStatus());

        return mapper.toDto(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getById(
            @NotNull @Positive Long id,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));

        accessChecker.checkUserAccess(payment.getUserId(), requesterId, roles);
        return mapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getByUserId(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        accessChecker.checkUserAccess(userId, requesterId, roles);
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getByOrderId(
            @NotNull @Positive Long orderId,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        accessChecker.checkAdminAccess(roles);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public PaymentResponseDto updateStatus(
            @NotNull @Positive Long id,
            @NotNull PaymentStatus newStatus,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        accessChecker.checkAdminAccess(roles);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));

        payment.setStatus(newStatus);
        return mapper.toDto(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(
            @NotNull @Positive Long id,
            @NotNull @Positive Long requesterId,
            @NotNull Set<UserRole> roles) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));

        accessChecker.checkUserAccess(payment.getUserId(), requesterId, roles);
        paymentRepository.delete(payment);
    }
}
