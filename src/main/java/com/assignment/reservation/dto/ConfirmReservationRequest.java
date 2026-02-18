package com.assignment.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/*@Data
public class ConfirmReservationRequest {
    @NotBlank
    private String customerName;

    @NotBlank
    private String roomNumber;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotBlank
    private String roomSegment; // SMALL, MEDIUM, LARGE, EXTRA_LARGE

    @NotBlank
    private String paymentMode; // CASH, BANK_TRANSFER, CREDIT_CARD

    private String paymentReference;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    @NotBlank
    private String currency;
}*/
public record ConfirmReservationRequest(
    @NotBlank String customerName,
    @NotBlank String roomNumber,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String roomSegment, // SMALL, MEDIUM, LARGE, EXTRA_LARGE
    @NotBlank String paymentMode, // CASH, BANK_TRANSFER, CREDIT_CARD
    String paymentReference,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank String currency
) {
}