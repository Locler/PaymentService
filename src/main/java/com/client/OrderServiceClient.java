package com.client;

import com.dtos.OrderDto;
import com.dtos.OrderWithUserDto;
import com.enums.UserRole;
import com.exceptionHandler.AccessDeniedException;
import com.exceptionHandler.OrderNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${order.service.base-url}")
    private String orderServiceBaseUrl;

    @Bean
    public Predicate<Throwable> orderServiceFailurePredicate() {
        Set<Class<?>> ignored = Set.of(
                com.exceptionHandler.OrderNotFoundException.class,
                com.exceptionHandler.AccessDeniedException.class
        );

        return ex -> ignored.stream().noneMatch(clazz -> clazz.isInstance(ex));
    }

    @CircuitBreaker(name = "orderServiceCircuitBreaker",
            fallbackMethod = "fallbackOrder")
    public OrderDto getOrderById(Long orderId, Long requesterId, Set<UserRole> roles) {

        log.info("OrderServiceBaseUrl = {} ", orderServiceBaseUrl);
        log.info("Fetching order with id = {} ", orderId);

        WebClient client = webClientBuilder.baseUrl(orderServiceBaseUrl).build();

        OrderWithUserDto order = client.get()
                .uri("/orders/{id}", orderId)
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", rolesToHeader(roles))
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        r -> Mono.error(new OrderNotFoundException(orderId))
                )
                .onStatus(
                        status -> status.value() == 403,
                        r -> Mono.error(new AccessDeniedException(orderId))
                )
                .bodyToMono(OrderWithUserDto.class)
                .block();

        log.info("Fetched order: {}", order);


        if (order == null || order.getOrder() == null) {
            throw new IllegalStateException("Order not found or response is empty");
        }

        return order.getOrder();
    }

    public OrderDto fallbackOrder(Long orderId, Long requesterId, Set<UserRole> roles, Throwable ex) {
        log.error("Order service unavailable for orderId={} due to {}", orderId, ex.toString());
        throw new IllegalStateException("Order service unavailable, cannot fetch orderId=" + orderId);
    }

    private String rolesToHeader(Set<UserRole> roles) {
        return String.join(",", roles.stream().map(Enum::name).toList());
    }
}
