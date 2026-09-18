package com.hellfire.team.controller;

import com.hellfire.model.RefundStatus;
import com.hellfire.payment.dto.RefundDecisionRequest;
import com.hellfire.payment.dto.RefundDto;
import com.hellfire.payment.dto.TeamRefundRequest;
import com.hellfire.payment.service.RefundService;
import com.hellfire.security.SensitiveDataAuditService;
import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Refund queue. Every team role can read; TEAM_ADMIN / TEAM_MANAGER decide. */
@RestController
@RequestMapping("/api/team/refunds")
@RequiredArgsConstructor
public class TeamRefundController {

    private final RefundService refundService;
    private final SensitiveDataAuditService auditService;

    @GetMapping
    public ResponseEntity<PageResponse<RefundDto>> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(refundService.list(status, q, page, size, TeamAuthorities.canRevealBank(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundDto> get(@PathVariable Long id, Authentication authentication) {
        boolean reveal = TeamAuthorities.canRevealBank(authentication);
        RefundDto dto = refundService.get(id, reveal);
        if (reveal && dto.getBankAccount() != null) {
            auditService.recordReveal(authentication, SensitiveDataAuditService.REFUND_BANK_ACCOUNT, id);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<RefundDto> create(@Valid @RequestBody TeamRefundRequest request, Authentication authentication) {
        return new ResponseEntity<>(refundService.create(request, authentication.getName(), true), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<RefundDto> approve(@PathVariable Long id,
                                             @RequestBody(required = false) @Valid RefundDecisionRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(refundService.approve(id, request, authentication.getName(), true));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<RefundDto> complete(@PathVariable Long id,
                                              @Valid @RequestBody RefundDecisionRequest request,
                                              Authentication authentication) {
        return ResponseEntity.ok(refundService.complete(id, request, authentication.getName(), true));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<RefundDto> reject(@PathVariable Long id,
                                            @Valid @RequestBody RefundDecisionRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(refundService.reject(id, request, authentication.getName(), true));
    }
}
