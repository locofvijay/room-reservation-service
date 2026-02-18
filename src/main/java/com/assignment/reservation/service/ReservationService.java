package com.assignment.reservation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assignment.reservation.dto.ConfirmReservationRequest;
import com.assignment.reservation.dto.ConfirmReservationResponse;
import com.assignment.reservation.dto.PaymentStatusResponse;
import com.assignment.reservation.entity.PaymentMode;
import com.assignment.reservation.entity.Reservation;
import com.assignment.reservation.entity.ReservationStatus;
import com.assignment.reservation.entity.RoomSegment;
import com.assignment.reservation.exception.InvalidReservationException;
import com.assignment.reservation.exception.PaymentNotConfirmedException;
import com.assignment.reservation.exception.ReservationNotFoundException;
import com.assignment.reservation.repository.ReservationRepository;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class ReservationService {
    private final ReservationRepository repo;
    private final CreditCardClient creditCardClient;

    public ReservationService(ReservationRepository repo, CreditCardClient creditCardClient) {
        this.repo = repo;
        this.creditCardClient = creditCardClient;
    }

    @Transactional
    public ConfirmReservationResponse confirm(ConfirmReservationRequest req) {
        log.info("Confirm reservation request for {} room {}", req.customerName(), req.roomNumber());

        long days = ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        if (days <= 0 || days > 30) {
             throw new InvalidReservationException("Reservation length must be between 1 and 30 days");
        }

        Reservation r = new Reservation();
        r.setId(generateId());
        r.setCustomerName(req.customerName());
        r.setRoomNumber(req.roomNumber());
        r.setStartDate(req.startDate());
        r.setEndDate(req.endDate());
        r.setRoomSegment(RoomSegment.valueOf(req.roomSegment().toUpperCase()));
        r.setPaymentMode(PaymentMode.valueOf(req.paymentMode().toUpperCase()));
        r.setPaymentReference(req.paymentReference());
        r.setAmount(req.amount());
        r.setCurrency(req.currency());
        /****
         * Reservation r = Reservation.builder()
         * .id(generateId())
         * .customerName(req.getCustomerName())
         * .roomNumber(req.getRoomNumber())
         * .startDate(req.getStartDate())
         * .endDate(req.getEndDate())
         * .roomSegment(RoomSegment.valueOf(req.getRoomSegment().toUpperCase()))
         * .paymentMode(PaymentMode.valueOf(req.getPaymentMode().toUpperCase()))
         * .paymentReference(req.getPaymentReference())
         * .amount(req.getAmount())
         * .currency(req.getCurrency())
         * .build();
         */

        if (r.getPaymentMode() == PaymentMode.CASH) {
            r.setStatus(ReservationStatus.CONFIRMED);
            repo.save(r);
            log.info("Reservation {} confirmed (cash)", r.getId());
            return new ConfirmReservationResponse(r.getId(), r.getStatus().name());
        } else if (r.getPaymentMode() == PaymentMode.CREDIT_CARD) {
            // blocking for simplicity - in prod use async/reactive or retries
            Mono<PaymentStatusResponse> mono = creditCardClient.getPaymentStatus(req.paymentReference());
            PaymentStatusResponse resp = mono.block();
            if (resp != null && "CONFIRMED".equalsIgnoreCase(resp.status())) {
                r.setStatus(ReservationStatus.CONFIRMED);
                repo.save(r);
                log.info("Reservation {} confirmed (credit-card)", r.getId());
                return new ConfirmReservationResponse(r.getId(), r.getStatus().name());
            } else {
                log.warn("Credit card payment not confirmed for ref {}", req.paymentReference());
                throw new PaymentNotConfirmedException("Credit card payment not confirmed");

            }
        } else {
            // BANK_TRANSFER
            r.setStatus(ReservationStatus.PENDING_PAYMENT);
            repo.save(r);
            log.info("Reservation {} pending payment (bank transfer)", r.getId());
            return new ConfirmReservationResponse(r.getId(), r.getStatus().name());
        }
    }

    @Transactional
    public void markConfirmedIfAmountMatches(String reservationId, BigDecimal amountReceived) {
        log.debug("Marking reservation {} confirmed if amount {} matches", reservationId, amountReceived);

        repo.findById(reservationId).ifPresentOrElse(r -> {
            if (r.getPaymentMode() == PaymentMode.BANK_TRANSFER &&
                    r.getStatus() == ReservationStatus.PENDING_PAYMENT) {
                if (amountReceived.compareTo(r.getAmount()) >= 0) {
                    r.setStatus(ReservationStatus.CONFIRMED);
                    repo.save(r);
                    log.info("Reservation {} confirmed via bank transfer", reservationId);
                } else {
                    log.info("Amount {} is less than expected {} for {}", amountReceived, r.getAmount(), reservationId);
                }
            } else {
                log.info("Reservation {} not eligible for bank transfer confirm (mode/status)", reservationId);
            }
        }, () -> log.warn("Reservation {} not found while processing payment event", reservationId));
    }
    @Transactional
    public void cancelStaleBankTransferReservations(LocalDate cutoffDate) {
        log.info("Cancelling bank-transfer reservations starting before {}", cutoffDate);
        List<Reservation> list = repo.findByStatusAndPaymentModeAndStartDateBefore(ReservationStatus.PENDING_PAYMENT,
                PaymentMode.BANK_TRANSFER, cutoffDate);
        list.forEach(r -> {
            r.setStatus(ReservationStatus.CANCELLED);
            repo.save(r);
            log.info("Reservation {} cancelled due to missing bank transfer", r.getId());
        });
    }

    public Reservation getById(String id) {
        return repo.findById(id).orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));
    }
    private String generateId() {
        return "R" + RandomStringUtils.randomAlphanumeric(7).toUpperCase();
    }

}
