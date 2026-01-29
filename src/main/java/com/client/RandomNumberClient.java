package com.client;

import com.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    @Value("${external.random.api.base-url}")
    private String baseUrl;

    @Value("${external.random.api.path}")
    private String path;

    @Value("${external.random.api.query}")
    private String query;

    private final WebClient.Builder webClientBuilder;


    public PaymentStatus resolvePaymentStatus() {
        try {
            Integer[] response = webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(path + query)
                    .retrieve()
                    .bodyToMono(Integer[].class)
                    .block();

            if (response == null || response.length == 0) {
                return PaymentStatus.FAILED;
            }

            return response[0] % 2 == 0
                    ? PaymentStatus.SUCCESS
                    : PaymentStatus.FAILED;

        } catch (Exception e) {
            return PaymentStatus.FAILED;
        }
    }
}
