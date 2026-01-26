package com.service;

import com.event.CreatePaymentEvent;
import com.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCreatePaymentEvent(CreatePaymentEvent event) {
        kafkaTemplate.send(KafkaTopics.CREATE_PAYMENT, event);
        log.info("Sent CREATE_PAYMENT event: {}", event);
    }
}
