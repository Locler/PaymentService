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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PaymentIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    static WireMockServer wireMock;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;


    @BeforeEach
    void setup() {
        paymentEventProducer = Mockito.mock(PaymentEventProducer.class);

        ReflectionTestUtils.setField(paymentService, "paymentEventProducer", paymentEventProducer);

        paymentRepository.deleteAll();
    }

    @BeforeAll
    void setupAll() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(paymentService, "paymentEventProducer", paymentEventProducer);

        wireMock = new WireMockServer(8089);
        wireMock.start();
        configureFor("localhost", 8089);

        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(okJson("""
                        {
                          "id": 1,
                          "name": "Test",
                          "surname": "User",
                          "email": "test@example.com",
                          "birthDate": "1990-01-01",
                          "active": true
                        }
                        """)));


        stubFor(get(urlEqualTo("/users/2"))
                .willReturn(okJson("""
                        {
                          "id": 2,
                          "name": "Inactive",
                          "surname": "User",
                          "email": "inactive@example.com",
                          "birthDate": "1990-01-01",
                          "active": false
                        }
                        """)));


        stubFor(get(urlEqualTo("/orders/10"))
                .willReturn(okJson("""
                        {
                          "id": 10,
                          "status": "NEW",
                          "deleted": false,
                          "totalPrice": 100.00
                        }
                        """)));

        stubFor(get(urlEqualTo("/orders/11"))
                .willReturn(okJson("""
                        {
                          "id": 11,
                          "status": "PAID",
                          "deleted": false,
                          "totalPrice": 100.00
                        }
                        """)));

        stubFor(get(urlEqualTo("/orders/12"))
                .willReturn(okJson("""
                        {
                          "id": 12,
                          "status": "NEW",
                          "deleted": true,
                          "totalPrice": 100.00
                        }
                        """)));
    }

    @AfterAll
    void teardownAll() {
        wireMock.stop();
    }

    @BeforeEach
    void cleanup() {
        paymentRepository.deleteAll();
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
        verify(paymentEventProducer, times(1)).sendCreatePaymentEvent(captor.capture());

        CreatePaymentEvent event = captor.getValue();
        assertThat(event.getPaymentId()).isEqualTo(response.getId());
        assertThat(event.getOrderId()).isEqualTo(dto.getOrderId());
        assertThat(event.getStatus()).isEqualTo(response.getStatus());
    }


    @Test
    void createPaymentFail() {

        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(11L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));

        assertThatThrownBy(() -> paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create payment for order with status PAID");
    }

    @Test
    void getById() {

        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(100));
        PaymentResponseDto saved = paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        PaymentResponseDto response = paymentService.getById(saved.getId(), 1L, Set.of(UserRole.ROLE_USER));
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(saved.getId());
    }

    @Test
    void getByIdFail() {
        assertThatThrownBy(() -> paymentService.getById(999L, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Payment not found");
    }


    @Test
    void getByUserId() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));
        paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        List<PaymentResponseDto> payments = paymentService.getByUserId(1L, 1L, Set.of(UserRole.ROLE_USER));
        assertThat(payments.size()).isEqualTo(1);
    }


    @Test
    void getByOrderIdForAdmin() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));
        paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        List<PaymentResponseDto> payments = paymentService.getByOrderId(10L, 99L, Set.of(UserRole.ROLE_ADMIN));
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    void getByOrderIdFail() {
        assertThatThrownBy(() -> paymentService.getByOrderId(10L, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(SecurityException.class);
    }


    @Test
    void updateStatus() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));
        PaymentResponseDto saved = paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        PaymentResponseDto updated = paymentService.updateStatus(saved.getId(), PaymentStatus.SUCCESS, 99L, Set.of(UserRole.ROLE_ADMIN));
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void updateStatusForNonAdmin() {
        assertThatThrownBy(() -> paymentService.updateStatus(1L, PaymentStatus.SUCCESS, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(SecurityException.class);
    }


    @Test
    void delete() {
        PaymentCreateRequestDto dto = new PaymentCreateRequestDto();
        dto.setUserId(1L);
        dto.setOrderId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(50));
        PaymentResponseDto saved = paymentService.createPayment(dto, 1L, Set.of(UserRole.ROLE_USER));

        paymentService.delete(saved.getId(), 1L, Set.of(UserRole.ROLE_USER));
        assertThat(paymentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteFail() {
        assertThatThrownBy(() -> paymentService.delete(999L, 1L, Set.of(UserRole.ROLE_USER)))
                .isInstanceOf(NoSuchElementException.class);
    }
}
