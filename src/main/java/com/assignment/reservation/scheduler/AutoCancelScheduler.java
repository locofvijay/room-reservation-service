package com.assignment.reservation.scheduler;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.assignment.reservation.service.ReservationService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class AutoCancelScheduler {
     private final ReservationService reservationService;

    @Value("${app.cancellation.days-before:2}")
    private int daysBefore;

    public AutoCancelScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // run every hour (for dev). Change to daily cron in prod.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cancelStale() {
        LocalDate cutoff = LocalDate.now().plusDays(daysBefore);
        log.debug("Running cancellation job with cutoff {}", cutoff);
        reservationService.cancelStaleBankTransferReservations(cutoff);
    }
}
