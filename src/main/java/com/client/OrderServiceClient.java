package com.client;

import com.dtos.OrderDto;
import com.dtos.OrderWithUserDto;
import com.enums.UserRole;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${order.service.base-url}")
    private String orderServiceBaseUrl;

    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackOrder")
    public OrderDto getOrderById(Long orderId, Long requesterId, Set<UserRole> roles) {

        log.info("OrderServiceBaseUrl = {} ", orderServiceBaseUrl);
        log.info("Fetching order with id = {} ", orderId);

        WebClient client = webClientBuilder.baseUrl(orderServiceBaseUrl).build();

        OrderWithUserDto order = client.get()
                .uri("/orders/{id}", orderId)
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", rolesToHeader(roles))
                .retrieve()
                .bodyToMono(OrderWithUserDto.class)
                .block();

        log.info("Fetched order: {}", order);


        if (order == null || order.getOrder() == null) {
            throw new IllegalStateException("Order not found or response is empty");
        }

        return order.getOrder();
    }

    public OrderDto fallbackOrder(Long orderId, Long requesterId, Set<UserRole> roles, Throwable ex) {
        throw new IllegalStateException("Order service unavailable, cannot fetch orderId=" + orderId);
    }

    private String rolesToHeader(Set<UserRole> roles) {
        return String.join(",", roles.stream().map(Enum::name).toList());
    }
}
