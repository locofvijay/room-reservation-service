package com.assignment.reservation.kafka;

import java.math.BigDecimal;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.assignment.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class BankTransferListener {
    private final ReservationService reservationService;
    private final ObjectMapper mapper = new ObjectMapper();

    public BankTransferListener(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @KafkaListener(topics = "bank-transfer-payment-update", groupId = "reservation-service-group")
    public void listen(String payload) {
        log.info("Received bank-transfer event: {}", payload);
        try {
            JsonNode n = mapper.readTree(payload);
            BigDecimal amount = new BigDecimal(n.path("amountReceived").asText("0"));
            String description = n.path("transactionDescription").asText("");
            String[] parts = description.trim().split("\\s+");
            String reservationId = parts.length > 1 ? parts[1] : null;
            if (reservationId != null) {
                reservationService.markConfirmedIfAmountMatches(reservationId, amount);
            } else {
                log.warn("Could not parse reservationId from description: {}", description);
            }
        } catch (Exception e) {
            log.error("Error parsing bank-transfer event", e);
        }
    }
}
