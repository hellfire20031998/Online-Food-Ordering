package com.hellfire.team.controller;

import com.hellfire.model.ApplicationStatus;
import com.hellfire.onboarding.dto.RejectApplicationRequest;
import com.hellfire.onboarding.dto.RestaurantApplicationDto;
import com.hellfire.onboarding.service.RestaurantApplicationService;
import com.hellfire.security.SensitiveDataAuditService;
import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Review queue for restaurant owner applications. Every team role can read; approving and
 * rejecting is for TEAM_ADMIN, TEAM_MANAGER and TEAM_RESTAURANT_SUPPORT. Bank details are
 * unmasked only for TEAM_ADMIN / TEAM_MANAGER.
 */
@RestController
@RequestMapping("/api/team/restaurant-applications")
@RequiredArgsConstructor
public class TeamApplicationController {

    private final RestaurantApplicationService applicationService;
    private final SensitiveDataAuditService auditService;

    @GetMapping
    public ResponseEntity<PageResponse<RestaurantApplicationDto>> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(applicationService.list(status, q, page, size, TeamAuthorities.canRevealBank(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantApplicationDto> get(@PathVariable Long id, Authentication authentication) {
        boolean reveal = TeamAuthorities.canRevealBank(authentication);
        RestaurantApplicationDto dto = applicationService.get(id, reveal);
        if (reveal && dto.getBankAccount() != null) {
            auditService.recordReveal(authentication, SensitiveDataAuditService.APPLICATION_BANK_ACCOUNT, id);
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize(TeamAuthorities.RESTAURANT_ONBOARDING)
    public ResponseEntity<RestaurantApplicationDto> approve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(applicationService.approve(id, authentication.getName(),
                TeamAuthorities.canRevealBank(authentication)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize(TeamAuthorities.RESTAURANT_ONBOARDING)
    public ResponseEntity<RestaurantApplicationDto> reject(@PathVariable Long id,
                                                           @Valid @RequestBody RejectApplicationRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(applicationService.reject(id, request.getReason(), authentication.getName(),
                TeamAuthorities.canRevealBank(authentication)));
    }
}
