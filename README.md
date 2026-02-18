

# Room Reservation Service

## Overview

`room-reservation-service` is a Spring Boot 3 microservice developed using Java 21.

It manages hotel room reservations for Marvel Hospitality Management Corporation and supports:

* Cash payments (immediate confirmation)
* Credit card payments (synchronous external API validation)
* Bank transfer payments (event-driven confirmation)
* Automatic cancellation of unpaid bank transfers

The application follows microservice and event-driven architecture principles.

---

## Architecture

### Key Components

* **Spring Boot 3** REST API
* **JPA + H2 (dev) / Postgres (prod)** for persistence
* **Kafka** for bank-transfer payment events
* **WireMock** (local dev) to mock credit-card-payment-service
* **Log4j2** for structured logging
* **Lombok** to reduce boilerplate
* **record** Immutable data classes, zero boilerplate. 
* **Custom Exception Handling** for consistent error responses
* **Docker Compose** for local environment setup

---

## Functional Requirements

### 1. Confirm Room Reservation

Endpoint:

```
POST /api/reservations/confirm
```

#### Payment Handling Logic

| Payment Mode  | Behavior                                                                        |
| ------------- | ------------------------------------------------------------------------------- |
| CASH          | Immediately confirmed                                                           |
| CREDIT_CARD   | Calls external credit-card-payment-service; confirms only if status = CONFIRMED |
| BANK_TRANSFER | Created as PENDING_PAYMENT                                                      |

#### Validation Rules

* Reservation cannot exceed 30 days
* Amount must be positive
* Valid room segment
* Valid payment mode

---

### 2. Bank Transfer Payment Update

Kafka Topic:

```
bank-transfer-payment-update
```

When a payment event is received:

* Extract reservationId from `transactionDescription`
* If `amountReceived >= reservation.amount`
* Mark reservation as CONFIRMED

---

### 3. Automatic Cancellation

Scheduled job runs periodically:

* Finds reservations with:

  * `paymentMode = BANK_TRANSFER`
  * `status = PENDING_PAYMENT`
  * `startDate <= today + 2 days`
* Updates status to CANCELLED

---

## Technology Stack

| Layer            | Technology                      |
| ---------------- | ------------------------------- |
| Language         | Java 21                         |
| Framework        | Spring Boot 3                   |
| Database         | H2 (dev), Postgres (prod-ready) |
| Messaging        | Apache Kafka                    |
| Logging          | Log4j2                          |
| API Client       | Spring WebClient                |
| Testing          | JUnit 5 + Testcontainers        |
| Build            | Maven                           |
| Containerization | Docker + Docker Compose         |

---

## Project Structure

```
src/main/java/com/example/reservation
│
├── controller          # REST endpoints
├── service             # Business logic
├── repository          # JPA repositories
├── entity              # Domain models
├── dto                 # Request/Response models
├── kafka               # Kafka consumers
├── scheduler           # Scheduled jobs
├── exception           # Custom exceptions
├── advice              # Global exception handler
└── config              # Configuration classes
```

---

## Running Locally

### Prerequisites

* Java 21
* Maven 3.9+
* Docker Desktop
* Docker Compose

---

## Step 1 – Build Application

```bash
mvn clean package
```

---

## Step 2 – Start Environment (Kafka + WireMock + App)

```bash
docker compose up --build
```

Services started:

| Service    | Port            |
| ---------- | --------------- |
| App        | 8080            |
| WireMock   | 8081            |
| Kafka      | 9092            |
| H2 Console | 8080/h2-console |

---

## Step 3 – Test Endpoints

### Cash Reservation

```bash
curl -X POST http://localhost:8080/api/reservations/confirm \
-H "Content-Type: application/json" \
-d '{
  "customerName":"Alice",
  "roomNumber":"101",
  "startDate":"2026-03-10",
  "endDate":"2026-03-12",
  "roomSegment":"SMALL",
  "paymentMode":"CASH",
  "amount":"100.00",
  "currency":"EUR"
}'
```

Expected:

```
CONFIRMED
```

---

### Credit Card Reservation

Use paymentReference = `CONFIRM_REF`

```bash
curl -X POST http://localhost:8080/api/reservations/confirm \
-H "Content-Type: application/json" \
-d '{
  "customerName":"Bob",
  "roomNumber":"102",
  "startDate":"2026-03-15",
  "endDate":"2026-03-16",
  "roomSegment":"MEDIUM",
  "paymentMode":"CREDIT_CARD",
  "paymentReference":"CONFIRM_REF",
  "amount":"200.00",
  "currency":"EUR"
}'
```

WireMock returns CONFIRMED → reservation confirmed.

---

### Bank Transfer Reservation

Create reservation (returns PENDING_PAYMENT).

Then publish Kafka event:

```json
{
  "paymentId":"PAY1",
  "debtorAccountnumber":"NL001234",
  "amountReceived":"150.00",
  "transactionDescription":"1401541457 RABC1234"
}
```

Use Kafka CLI to publish to:

```
bank-transfer-payment-update
```

Reservation will transition to CONFIRMED.

---

## Logging

* Console logging via Log4j2
* File logging: `logs/app.log`
* Log levels:

  * INFO – Business events
  * DEBUG – Detailed flow
  * WARN – Validation issues
  * ERROR – System failures

---

## Error Handling

Centralized via:

```
@RestControllerAdvice
```

Error Response Format:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Credit card payment not confirmed"
}
```

---

## Database

* H2 in-memory database (dev)
* Accessible at:

```
http://localhost:8080/h2-console
```

JDBC URL:

```
jdbc:h2:mem:reservations
```

---

## Testing

### Unit Tests
* Location: src/test/java/**/*Test.java
* Execution: Run via the Maven Surefire Plugin.
* Scope: Fast execution; must not rely on external systems or resources.
### Integration Tests
* Location: src/test/java/**/*IT.java
* Execution: Run via the Maven Failsafe Plugin (using mvn verify).
* Scope: End-to-end flows; may initialize containers, databases, or embedded servers.
### Contract Tests
* Status: Replaced Spring Cloud Contract Stub Runner with embedded WireMock.
* Scope: Simulates contract-like behavior and external API dependencies when a physical stub JAR is unavailable. 

### Naming Convention

| Test Type         | Naming Pattern | Maven Plugin |
| ----------------- | -------------- | ------------ |
| Unit Tests        | `*Test.java`   | Surefire     |
| Integration Tests | `*IT.java`     | Failsafe     |

Examples:

* `ReservationServiceTest.java` → Unit test
* `KafkaConsumerIT.java` → Integration test
* `CreditCardContractIT.java` → Integration / Contract test


## Run Tests (Commands)

### ▶ Run Unit Tests Only

```bash
mvn test
```

Or explicitly:

```bash
mvn -DskipITs=true test
```

---

### ▶ Run Integration Tests Only

```bash
mvn -DskipTests=true verify
```

---

### ▶ Run All Tests (Recommended)

```bash
mvn verify
```

This runs:

1. Unit tests (Surefire)
2. Integration tests (Failsafe)

---

### ▶ Force Dependency Updates

If Maven caches a failed dependency:

```bash
mvn -U clean verify
```

---

## Design Highlights

* Clean separation of concerns
* Event-driven architecture for bank transfers
* Synchronous validation for credit cards
* BigDecimal for monetary accuracy
* Centralized exception management
* Structured logging
* Production-ready extensible design

---

## Production Considerations

* Replace H2 with PostgreSQL
* Add OAuth2 authentication
* Add Resilience4j for external API resilience
* Add OpenTelemetry for distributed tracing
* Add Prometheus metrics
* Use Kubernetes deployment with health checks
* Configure Kafka with proper security and replication

---

## End-to-End Flow Summary

1. User submits reservation request
2. Service validates input
3. Based on payment mode:

   * Confirm immediately
   * Call external API
   * Mark pending
4. Kafka event updates bank transfers
5. Scheduler auto-cancels stale reservations
6. Logs + DB updated consistently

---

## Author

Marvel Hospitality Management – Room Reservation Microservice
Built with Spring Boot 3 + Java 21

---
