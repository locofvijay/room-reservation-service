package com.assignment.reservation.it;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.assignment.reservation.entity.Reservation;
import com.assignment.reservation.entity.ReservationStatus;
import com.assignment.reservation.repository.ReservationRepository;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KafkaConsumerIT {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));


    // Removing the "PLAINTEXT://" prefix because Spring/Kafka client expects host:port
    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers().replaceFirst("(?i)^.*://", ""));
        r.add("spring.kafka.listener.auto-startup", () -> "true"); // <--- enable listeners for test
    }

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ReservationRepository repo;

    @BeforeAll
    static void beforeAll() {
        String bootstrap = kafka.getBootstrapServers().replaceFirst("(?i)^.*://", "");
        if (bootstrap == null || bootstrap.isBlank()) {
            throw new IllegalStateException("Kafka bootstrap servers not available from Testcontainers");
        }

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        try (AdminClient client = AdminClient.create(props)) {
            NewTopic topic = new NewTopic("bank-transfer-payment-update", 1, (short) 1);
            try {
                client.createTopics(java.util.List.of(topic)).all().get(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while creating topic", ie);
            } catch (ExecutionException | TimeoutException ee) {
                throw new RuntimeException("Failed to create topic (admin client)", ee);
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // continue; it's just a short pause
        }
    }

    @Test
    void whenBankTransferEventArrives_reservationBecomesConfirmed() {
        Reservation toSave = new Reservation();
        toSave.setId(UUID.randomUUID().toString());
        toSave.setCustomerName("Test");
        toSave.setRoomNumber("200");
        toSave.setStartDate(java.time.LocalDate.now().plusDays(1));
        toSave.setEndDate(java.time.LocalDate.now().plusDays(2));
        toSave.setAmount(new java.math.BigDecimal("150.00"));
        toSave.setCurrency("EUR");
        toSave.setStatus(ReservationStatus.CONFIRMED);

        var reservation = repo.save(toSave);

        String reservationId = reservation.getId();
        String payload = "{\"paymentId\":\"PAY-1\",\"debtorAccountnumber\":\"NL001234\",\"amountReceived\":\"150.00\",\"transactionDescription\":\"1401541457 "
                + reservationId + "\"}";

        kafkaTemplate.send("bank-transfer-payment-update", reservationId, payload);
        kafkaTemplate.flush();

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            var r = repo.findById(reservationId).orElseThrow();
            assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
        });
    }
}
