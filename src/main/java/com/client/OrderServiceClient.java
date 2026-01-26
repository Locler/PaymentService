package com.client;

import com.dtos.OrderDto;
import com.enums.UserRole;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${order.service.base-url}")
    private String orderServiceBaseUrl;

    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackOrder")
    public OrderDto getOrderById(Long orderId, Long requesterId, Set<UserRole> roles) {

        WebClient client = webClientBuilder.baseUrl(orderServiceBaseUrl).build();

        OrderDto order = client.get()
                .uri("/{id}", orderId)
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", rolesToHeader(roles))
                .retrieve()
                .bodyToMono(OrderDto.class)
                .block();

        if (order == null) {
            throw new IllegalStateException("Order not found");
        }

        return order;
    }

    public OrderDto fallbackOrder(Long orderId, Long requesterId, Set<UserRole> roles, Throwable ex) {
        throw new IllegalStateException("Order service unavailable, cannot fetch orderId=" + orderId);
    }

    private String rolesToHeader(Set<UserRole> roles) {
        return String.join(",", roles.stream().map(Enum::name).toList());
    }
}
