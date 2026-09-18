package com.hellfire.team.controller;

import com.hellfire.model.OrderStatus;
import com.hellfire.model.UserStatus;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.payment.service.CustomerBankAccountService;
import com.hellfire.security.SensitiveDataAuditService;
import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PageResponse;
import com.hellfire.team.dto.TeamCustomerDto;
import com.hellfire.team.dto.TeamOrderDto;
import com.hellfire.team.service.TeamDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team/customers")
@RequiredArgsConstructor
public class TeamCustomerController {

    private final TeamDirectoryService directoryService;
    private final CustomerBankAccountService bankAccountService;
    private final SensitiveDataAuditService auditService;

    @GetMapping
    public ResponseEntity<PageResponse<TeamCustomerDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(directoryService.listCustomers(q, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamCustomerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(directoryService.getCustomer(id));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<PageResponse<TeamOrderDto>> orders(
            @PathVariable Long id,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        directoryService.getCustomer(id); // 404 if unknown
        return ResponseEntity.ok(directoryService.listOrders(status, null, id, null, null, page, size));
    }

    /** Saved refund account, unmasked; admin/manager only, every view is recorded in the access log. */
    @GetMapping("/{id}/bank-account")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<BankAccountDto> bankAccount(@PathVariable Long id, Authentication authentication) {
        directoryService.getCustomer(id); // 404 if unknown or not a customer
        BankAccountDto dto = bankAccountService.getForTeam(id);
        auditService.recordReveal(authentication, SensitiveDataAuditService.CUSTOMER_BANK_ACCOUNT, id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/block")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<TeamCustomerDto> block(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(directoryService.blockCustomer(id, authentication.getName()));
    }

    @PutMapping("/{id}/unblock")
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<TeamCustomerDto> unblock(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(directoryService.unblockCustomer(id, authentication.getName()));
    }
}
