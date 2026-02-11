package com.assignment.reservation.controller;

import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

import com.assignment.reservation.service.ReservationService;
import com.assignment.reservation.dto.ConfirmReservationRequest;
import com.assignment.reservation.dto.ConfirmReservationResponse;
import com.assignment.reservation.entity.Reservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Log4j2
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @GetMapping("/")
    public String getControllerRoot()   {
        return "Welcome to Room Reservation Service!";
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmReservationResponse> confirm(@Valid @RequestBody ConfirmReservationRequest req) {
        ConfirmReservationResponse resp = service.confirm(req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> get(@PathVariable String id) {
        Reservation r = service.getById(id);
        return ResponseEntity.ok(r);
    }
}
