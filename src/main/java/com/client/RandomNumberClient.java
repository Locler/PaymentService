package com.client;

import com.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    @Value("${external.random.api.path}")
    private String path;

    @Value("${external.random.api.query}")
    private String query;

    private final WebClient.Builder webClientBuilder;


    public PaymentStatus resolvePaymentStatus() {
        try {

            String uri = path + "?" + query;

            Integer number = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();

            if (number == null) {
                return PaymentStatus.FAILED;
            }

            return number % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        } catch (Exception ex) {
            return PaymentStatus.FAILED;
        }
    }
}
