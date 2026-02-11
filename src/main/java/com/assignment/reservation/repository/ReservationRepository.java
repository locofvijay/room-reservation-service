package com.assignment.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.assignment.reservation.entity.Reservation;
import com.assignment.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByStatusAndPaymentModeAndStartDateBefore(ReservationStatus status,
            com.assignment.reservation.entity.PaymentMode mode,
            LocalDate date);
}
