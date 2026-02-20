package com.assignment.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.assignment.reservation.dto.ConfirmReservationRequest;
import com.assignment.reservation.dto.ConfirmReservationResponse;
import com.assignment.reservation.dto.PaymentStatusResponse;
import com.assignment.reservation.entity.Reservation;
import com.assignment.reservation.entity.ReservationStatus;
import com.assignment.reservation.exception.PaymentNotConfirmedException;
import com.assignment.reservation.repository.ReservationRepository;

import reactor.core.publisher.Mono;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    ReservationRepository repo;
    @Mock
    CreditCardClient creditCardClient;

    @InjectMocks
    ReservationService service; // your service


    @Test
    void whenCashPayment_thenReservationConfirmed() {
        ConfirmReservationRequest req = new ConfirmReservationRequest(
                "Alice",
                "101", 
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                "SMALL",
                "CASH",
                null,
                new BigDecimal("100.00"),
                "EUR");

        when(repo.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ConfirmReservationResponse resp = service.confirm(req);

        assertThat(ReservationStatus.valueOf(resp.reservationStatus())).isEqualTo(ReservationStatus.CONFIRMED);
        verify(repo).save(argThat(r -> r.getStatus() == ReservationStatus.CONFIRMED));
    }



    @Test
    void whenCreditCardRejected_thenThrows() {
        ConfirmReservationRequest req = new ConfirmReservationRequest(
                "Bob", "102",
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(3),
                "SMALL",
                "CREDIT_CARD", "PAY-REF-1",
                new BigDecimal("50.00"), "EUR");

        when(creditCardClient.getPaymentStatus("PAY-REF-1"))
                .thenReturn(Mono.just(new PaymentStatusResponse("2026-02-17T10:00:00Z","REJECTED")));

        assertThatThrownBy(() -> service.confirm(req))
                .isInstanceOf(PaymentNotConfirmedException.class)
                .hasMessageContaining("not confirmed"); 
    }
}
   