package com.hellfire.team.controller;

import com.hellfire.model.PayoutStatus;
import com.hellfire.payment.dto.GeneratePayoutsRequest;
import com.hellfire.payment.dto.MarkPayoutPaidRequest;
import com.hellfire.payment.dto.PayoutDto;
import com.hellfire.payment.service.PayoutService;
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

import java.util.List;

/** Restaurant payouts. TEAM_ADMIN / TEAM_MANAGER only, including reads. */
@RestController
@RequestMapping("/api/team/payouts")
@RequiredArgsConstructor
@PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
public class TeamPayoutController {

    private final PayoutService payoutService;
    private final SensitiveDataAuditService auditService;

    @GetMapping
    public ResponseEntity<PageResponse<PayoutDto>> list(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(payoutService.list(status, restaurantId, page, size, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayoutDto> get(@PathVariable Long id, Authentication authentication) {
        PayoutDto dto = payoutService.get(id, true);
        if (dto.getBankAccount() != null) {
            auditService.recordReveal(authentication, SensitiveDataAuditService.PAYOUT_BANK_ACCOUNT, id);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/generate")
    public ResponseEntity<List<PayoutDto>> generate(@Valid @RequestBody GeneratePayoutsRequest request,
                                                    Authentication authentication) {
        return new ResponseEntity<>(
                payoutService.generate(request.getFrom(), request.getTo(), authentication.getName(), true),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<PayoutDto> markPaid(@PathVariable Long id,
                                              @Valid @RequestBody MarkPayoutPaidRequest request,
                                              Authentication authentication) {
        return ResponseEntity.ok(payoutService.markPaid(id, request.getReferenceNumber(), request.getNotes(),
                authentication.getName(), true));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<PayoutDto> cancel(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(payoutService.cancel(id, authentication.getName(), true));
    }
}
