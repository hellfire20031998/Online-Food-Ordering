package com.hellfire.team.controller;

import com.hellfire.model.OrderStatus;
import com.hellfire.team.dto.PageResponse;
import com.hellfire.team.dto.TeamOrderDto;
import com.hellfire.team.service.TeamDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Cross-restaurant order view. Read-only in phase 1; refunds arrive with the payment ledger. */
@RestController
@RequestMapping("/api/team/orders")
@RequiredArgsConstructor
public class TeamOrderController {

    private final TeamDirectoryService directoryService;

    @GetMapping
    public ResponseEntity<PageResponse<TeamOrderDto>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Date fromDate = from == null ? null : toDate(from);
        Date toExclusive = to == null ? null : toDate(to.plusDays(1));
        return ResponseEntity.ok(
                directoryService.listOrders(status, restaurantId, customerId, fromDate, toExclusive, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamOrderDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(directoryService.getOrder(id));
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
