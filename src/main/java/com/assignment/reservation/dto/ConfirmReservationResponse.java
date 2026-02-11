package com.assignment.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReservationResponse {
    private String reservationId;
    private String reservationStatus;
}
