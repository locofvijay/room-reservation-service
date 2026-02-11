package com.assignment.reservation.exception;

public class InvalidReservationException extends RuntimeException {
    public InvalidReservationException(String message){
        super(message);
    }
}
