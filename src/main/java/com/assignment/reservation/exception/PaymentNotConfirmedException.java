package com.assignment.reservation.exception;

public class PaymentNotConfirmedException extends RuntimeException{
    public PaymentNotConfirmedException(String message){
        super(message);
    }

}
