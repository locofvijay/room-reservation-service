package com.assignment.reservation.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Integration test that uses an embedded WireMock server instead of
 * spring-cloud-contract stub-runner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CreditCardContractIT {

    // wiremock port chosen to not collide with any docker-compose wiremock 
    static final int WIREMOCK_PORT = 8089;
    static WireMockServer wireMockServer;

    @Autowired
    TestRestTemplate restTemplate; 

    static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void startWiremock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().port(WIREMOCK_PORT).usingFilesUnderClasspath("wiremock"));
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/host/credit-card-payment-api/payment-status"))
                .withRequestBody(WireMock.matchingJsonPath("$[?(@.paymentReference == 'CONFIRM_REF')]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"lastUpdateDate\": \"2026-02-10T00:00:00Z\", \"status\": \"CONFIRMED\" }")));

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/host/credit-card-payment-api/payment-status"))
                .atPriority(10)
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"lastUpdateDate\": \"2026-02-10T00:00:00Z\", \"status\": \"REJECTED\" }")));
    }

    @AfterAll
    static void stopWiremock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // base URL that the application uses to call credit-card API
        registry.add("creditcard.api.base-url",
                () -> "http://localhost:" + WIREMOCK_PORT + "/host/credit-card-payment-api");
    }

    @Test
    void stubRespondsAsContract_confirm() throws Exception {
        var request = new java.util.HashMap<String, String>();
        request.put("paymentReference", "CONFIRM_REF");
        request.put("otherField", "x");

        var response = restTemplate.postForEntity(
                "http://localhost:" + WIREMOCK_PORT + "/host/credit-card-payment-api/payment-status",
                request,
                String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        JsonNode bodyJson = MAPPER.readTree(response.getBody());
        assertThat(bodyJson.path("status").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void stubRespondsAsContract_reject() throws Exception {
        var request = new java.util.HashMap<String, String>();
        request.put("paymentReference", "SOME_OTHER_REF");

        var response = restTemplate.postForEntity(
                "http://localhost:" + WIREMOCK_PORT + "/host/credit-card-payment-api/payment-status",
                request,
                String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        JsonNode bodyJson = MAPPER.readTree(response.getBody());
        assertThat(bodyJson.path("status").asText()).isEqualTo("REJECTED");
    }
}
