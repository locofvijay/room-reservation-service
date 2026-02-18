package com.assignment.reservation.dto;

/*@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private String lastUpdateDate;
    private String status; // CONFIRMED / REJECTED
} */

public record PaymentStatusResponse(String lastUpdateDate, String status) {
}
