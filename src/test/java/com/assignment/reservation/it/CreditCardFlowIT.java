package com.assignment.reservation.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.assignment.reservation.dto.ConfirmReservationRequest;
import com.assignment.reservation.dto.ConfirmReservationResponse;
import com.assignment.reservation.dto.PaymentStatusResponse;
import com.assignment.reservation.service.CreditCardClient;
import com.github.tomakehurst.wiremock.WireMockServer;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.credit-card.payment-service-url=http://localhost:8089" ,
        "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
class CreditCardFlowIT {

    static WireMockServer wireMock = new WireMockServer(8089);

    @BeforeAll
    static void beforeAll() {
        wireMock.start();
    }

    @AfterAll
    static void afterAll() {
        wireMock.stop();
    }

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;

    @MockitoBean
    CreditCardClient creditCardClient;

    @BeforeEach
    void setup() {
        when(creditCardClient.getPaymentStatus(anyString()))
            .thenReturn(Mono.just(new PaymentStatusResponse("2026-02-01T00:00:00Z", "CONFIRMED")));
    }

    @Test
    void whenCreditCardConfirmed_thenReservationConfirmed() {
        // stub credit-card service
        wireMock.stubFor(post(urlEqualTo("/host/credit-card-payment-api/payment-status"))
            .withRequestBody(matchingJsonPath("$.paymentReference", equalTo("CC-REF-1")))
            .willReturn(okJson("{\"status\":\"CONFIRMED\",\"lastUpdateDate\":\"2026-02-01T00:00:00Z\"}")));

        // prepare request
        var req = new ConfirmReservationRequest( 
            "Alice",                 
            "101",                    
            LocalDate.now().plusDays(5), 
            LocalDate.now().plusDays(6), 
            "SMALL",                 
            "CREDIT_CARD",            
            "CC-REF-1",             
            new BigDecimal("100.00"), 
            "EUR"  
        );                   

        var response = rest.postForEntity("/api/reservations/confirm", req, ConfirmReservationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().reservationStatus()).isEqualTo("CONFIRMED");
    }
}
