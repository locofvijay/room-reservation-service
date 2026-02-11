package com.assignment.reservation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.assignment.reservation.dto.PaymentStatusRequest;
import com.assignment.reservation.dto.PaymentStatusResponse;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@Component
public class CreditCardClient {
    private final WebClient webClient;
    private final String endpoint;

    public CreditCardClient(WebClient.Builder builder,
                            @Value("${app.credit-card.payment-service-url}") String endpoint) {
        this.webClient = builder.build();
        this.endpoint = endpoint;
    }

    public Mono<PaymentStatusResponse> getPaymentStatus(String paymentReference) {
        log.debug("Calling credit-card service for ref={}", paymentReference);
        PaymentStatusRequest req = new PaymentStatusRequest(paymentReference);
        return webClient.post()
                .uri(endpoint)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(PaymentStatusResponse.class)
                .doOnError(e -> log.warn("Credit-card call failed for ref {}: {}", paymentReference, e.getMessage()));
    }
}
