package com.client;

import com.dtos.UserInfoDto;
import com.enums.UserRole;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "fallbackUserById")
    public UserInfoDto getUserById(Long userId, Long requesterId, Set<UserRole> roles) {

        WebClient client = webClientBuilder.baseUrl(userServiceBaseUrl).build();

        UserInfoDto user = client.get()
                .uri("/{id}", userId)
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Roles", rolesToHeader(roles))
                .retrieve()
                .bodyToMono(UserInfoDto.class)
                .block();

        if (user == null) {
            throw new IllegalStateException("User not found");
        }

        return user;
    }

    public UserInfoDto fallbackUserById(Long userId, Long requesterId, Set<UserRole> roles, Throwable ex) {
        return UserInfoDto.builder()
                .name("Unknown")
                .surname("User")
                .email("unavailable")
                .active(false)
                .build();
    }

    private String rolesToHeader(Set<UserRole> roles) {
        return String.join(",", roles.stream().map(Enum::name).toList());
    }
}
