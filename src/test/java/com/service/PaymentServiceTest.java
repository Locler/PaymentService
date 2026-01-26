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
import com.event.CreatePaymentEvent;
import com.mapper.PaymentMapper;
import com.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private AccessChecker accessChecker;

    @Test
    void createPayment() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.TEN);

        UserInfoDto user = new UserInfoDto();
        user.setId(1L);
        user.setActive(true);

        OrderDto order = new OrderDto();
        order.setId(10L);
        order.setStatus("NEW");
        order.setDeleted(false);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(userServiceClient.getUserById(any(), any(), any())).thenReturn(user);
        when(orderServiceClient.getOrderById(any(), any(), any())).thenReturn(order);
        when(mapper.toEntity(dto)).thenReturn(new Payment());
        when(randomNumberClient.resolvePaymentStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(mapper.toDto(payment)).thenReturn(new PaymentResponseDto());

        PaymentResponseDto result =
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        verify(paymentEventProducer).sendCreatePaymentEvent(any(CreatePaymentEvent.class));
        verify(paymentRepository).save(any());
        Assertions.assertNotNull(result);
    }

    @Test
    void getById() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setUserId(10L);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(mapper.toDto(payment)).thenReturn(new PaymentResponseDto());

        PaymentResponseDto result =
                paymentService.getById(1L, 10L, Set.of(UserRole.ROLE_USER));

        verify(accessChecker).checkUserAccess(10L, 10L, Set.of(UserRole.ROLE_USER));
        Assertions.assertNotNull(result);
    }

    @Test
    void getByIdNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> paymentService.getById(1L, 1L, Set.of(UserRole.ROLE_USER)));
    }

    @Test
    void getByUserId() {
        Payment payment = new Payment();
        payment.setUserId(1L);

        when(paymentRepository.findByUserId(1L))
                .thenReturn(List.of(payment));
        when(mapper.toDto(payment))
                .thenReturn(new PaymentResponseDto());

        List<PaymentResponseDto> result =
                paymentService.getByUserId(1L, 1L, Set.of(UserRole.ROLE_USER));

        verify(accessChecker).checkUserAccess(1L, 1L, Set.of(UserRole.ROLE_USER));
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void updateStatus() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.NEW);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment))
                .thenReturn(payment);
        when(mapper.toDto(payment))
                .thenReturn(new PaymentResponseDto());

        PaymentResponseDto result =
                paymentService.updateStatus(
                        1L,
                        PaymentStatus.SUCCESS,
                        1L,
                        Set.of(UserRole.ROLE_ADMIN)
                );

        verify(accessChecker).checkAdminAccess(Set.of(UserRole.ROLE_ADMIN));
        Assertions.assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        Assertions.assertNotNull(result);
    }

    @Test
    void createPaymentAmount() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setPaymentAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER)));
    }

    @Test
    void deletePayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setUserId(10L);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        paymentService.delete(1L, 10L, Set.of(UserRole.ROLE_USER));

        verify(accessChecker).checkUserAccess(10L, 10L, Set.of(UserRole.ROLE_USER));
        verify(paymentRepository).delete(payment);
    }

}
