package com.assignment.reservation.dto;

/*@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReservationResponse {
    private String reservationId;
    private String reservationStatus;
}*/
public record ConfirmReservationResponse(String reservationId, String reservationStatus) {
}
