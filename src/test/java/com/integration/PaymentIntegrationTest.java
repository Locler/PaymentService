package com.integration;

import com.dtos.PaymentCreateRequestDto;
import com.dtos.PaymentResponseDto;
import com.enums.PaymentStatus;
import com.enums.UserRole;
import com.event.CreatePaymentEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.repository.PaymentRepository;
import com.service.PaymentEventProducer;
import com.service.PaymentService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    private static WireMockServer wireMock;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private PaymentEventProducer paymentEventProducer;

    @BeforeAll
    void setupAll() {

        mongo.start();
        System.setProperty("spring.data.mongodb.uri", mongo.getReplicaSetUrl());


        wireMock = new WireMockServer(8089);
        wireMock.start();
        configureFor("localhost", 8089);

        System.setProperty("user.service.base-url", "http://localhost:8089");
        System.setProperty("order.service.base-url", "http://localhost:8089");
    }

    @BeforeEach
    void setup() {
        wireMock.resetAll();

        paymentRepository.deleteAll();

        paymentEventProducer = mock(PaymentEventProducer.class);
        ReflectionTestUtils.setField(paymentService, "paymentEventProducer", paymentEventProducer);

        stubUserService();
        stubOrderService();
    }

    @AfterAll
    void teardown() {
        wireMock.stop();
        mongo.stop();
    }

    private void stubUserService() {
        stubFor(get(urlPathMatching("/users/1"))
                .willReturn(okJson("""
                    {
                      "id": 1,
                      "active": true
                    }
                """)));

        stubFor(get(urlPathMatching("/users/2"))
                .willReturn(okJson("""
                    {
                      "id": 2,
                      "active": false
                    }
                """)));
    }

    private void stubOrderService() {
        stubFor(get(urlPathMatching("/orders/10"))
                .willReturn(okJson("""
                    {
                      "order": {
                        "id": 10,
                        "status": "NEW",
                        "deleted": false
                      }
                    }
                """)));

        stubFor(get(urlPathMatching("/orders/11"))
                .willReturn(okJson("""
                    {
                      "order": {
                        "id": 11,
                        "status": "PAID",
                        "deleted": false
                      }
                    }
                """)));

        stubFor(get(urlPathMatching("/orders/12"))
                .willReturn(okJson("""
                    {
                      "order": {
                        "id": 12,
                        "status": "NEW",
                        "deleted": true
                      }
                    }
                """)));
    }

    @Test
    void createPayment() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(100));

        PaymentResponseDto response =
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        assertThat(response).isNotNull();
        assertThat(paymentRepository.findById(response.getId())).isPresent();

        ArgumentCaptor<CreatePaymentEvent> captor = ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(paymentEventProducer).sendCreatePaymentEvent(captor.capture());

        assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
    }

    @Test
    void createPaymentFail() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(11L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        assertThatThrownBy(() ->
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getById() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(100));

        PaymentResponseDto saved =
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        PaymentResponseDto response =
                paymentService.getById(saved.getId(), 1L, Set.of(UserRole.ROLE_USER));

        assertThat(response.getId()).isEqualTo(saved.getId());
    }

    @Test
    void getByIdFail() {
        assertThatThrownBy(() ->
                paymentService.getById(999L, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getByUserId() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        List<PaymentResponseDto> payments =
                paymentService.getByUserId(1L, 1L, Set.of(UserRole.ROLE_USER));

        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    void getByOrderId() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        List<PaymentResponseDto> payments =
                paymentService.getByOrderId(10L, 99L, Set.of(UserRole.ROLE_ADMIN));

        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    void updateStatus() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        PaymentResponseDto saved =
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        PaymentResponseDto updated =
                paymentService.updateStatus(
                        saved.getId(),
                        PaymentStatus.SUCCESS,
                        99L,
                        Set.of(UserRole.ROLE_ADMIN)
                );

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void delete() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        PaymentResponseDto saved =
                paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        paymentService.delete(saved.getId(), 1L, Set.of(UserRole.ROLE_USER));

        assertThat(paymentRepository.findById(saved.getId())).isEmpty();
    }
}