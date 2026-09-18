package com.hellfire.team.controller;

import com.hellfire.model.OrderStatus;
import com.hellfire.model.RestaurantStatus;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.onboarding.service.BankAccountService;
import com.hellfire.security.SensitiveDataAuditService;
import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PageResponse;
import com.hellfire.team.dto.TeamOrderDto;
import com.hellfire.team.dto.TeamRestaurantDto;
import com.hellfire.team.service.TeamDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team/restaurants")
@RequiredArgsConstructor
public class TeamRestaurantController {

    private final TeamDirectoryService directoryService;
    private final BankAccountService bankAccountService;
    private final SensitiveDataAuditService auditService;

    @GetMapping
    public ResponseEntity<PageResponse<TeamRestaurantDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RestaurantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(directoryService.listRestaurants(q, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamRestaurantDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(directoryService.getRestaurant(id));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<PageResponse<TeamOrderDto>> orders(
            @PathVariable Long id,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        directoryService.getRestaurant(id); // 404 if unknown
        return ResponseEntity.ok(directoryService.listOrders(status, id, null, null, null, page, size));
    }

    /** Payout account on file; unmasked, hence admin/manager only. */
    @GetMapping("/{id}/bank-account")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<BankAccountDto> bankAccount(@PathVariable Long id, Authentication authentication) {
        directoryService.getRestaurant(id); // 404 if unknown
        BankAccountDto dto = bankAccountService.getForTeam(id);
        auditService.recordReveal(authentication, SensitiveDataAuditService.RESTAURANT_BANK_ACCOUNT, id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<TeamRestaurantDto> suspend(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(directoryService.suspendRestaurant(id, authentication.getName()));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<TeamRestaurantDto> reactivate(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(directoryService.reactivateRestaurant(id, authentication.getName()));
    }
}
