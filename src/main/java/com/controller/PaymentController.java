package com.controller;

import com.dtos.PaymentCreateRequestDto;
import com.dtos.PaymentResponseDto;
import com.enums.PaymentStatus;
import com.enums.UserRole;
import com.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private Set<UserRole> parseRoles(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());
    }

    private final PaymentService service;

    @Autowired
    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> create(
            @RequestBody @Valid PaymentCreateRequestDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        PaymentResponseDto payment = service.createPayment(dto, requesterId, roles);
        return ResponseEntity.status(201).body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        PaymentResponseDto payment = service.getById(id, requesterId, roles);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/byUser/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getByUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        List<PaymentResponseDto> payments = service.getByUserId(userId, requesterId, roles);
        return ResponseEntity.ok(payments);
    }

    // ADMIN ONLY
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<PaymentResponseDto>> getByOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        List<PaymentResponseDto> payments = service.getByOrderId(orderId, requesterId, roles);
        return ResponseEntity.ok(payments);
    }

    // ADMIN ONLY
    @PutMapping("/{id}/status")
    public ResponseEntity<PaymentResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        PaymentResponseDto updated = service.updateStatus(id, status, requesterId, roles);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<UserRole> roles = parseRoles(rolesHeader);
        service.delete(id, requesterId, roles);
        return ResponseEntity.noContent().build();
    }

}