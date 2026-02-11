package com.assignment.reservation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    @Id
    private String id; // keeping as string for 8-char requirement later

    private String customerName;
    private String roomNumber;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private RoomSegment roomSegment;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String paymentReference; // user-provided

    @Column(precision = 10, scale = 4)
    private BigDecimal amount; // total amount expected

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private String currency;

}
