package com.assignment.reservation.dto;

/*@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusRequest {
    private String paymentReference;
}*/
public record PaymentStatusRequest(String paymentReference) {
}
