package com.hellfire.team.controller;

import com.hellfire.model.SensitiveDataAccessLog;
import com.hellfire.security.KeyRotationService;
import com.hellfire.security.SensitiveDataAuditService;
import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Encryption key status, re-encryption after rotation, and the sensitive-data access log. TEAM_ADMIN only. */
@RestController
@RequestMapping("/api/team/security")
@RequiredArgsConstructor
@PreAuthorize(TeamAuthorities.ADMIN_ONLY)
public class TeamSecurityController {

    private final KeyRotationService keyRotationService;
    private final SensitiveDataAuditService auditService;

    @GetMapping("/encryption")
    public ResponseEntity<KeyRotationService.Status> encryptionStatus() {
        return ResponseEntity.ok(keyRotationService.status());
    }

    @PostMapping("/encryption/rotate")
    public ResponseEntity<KeyRotationService.Report> reencrypt(Authentication authentication) {
        return ResponseEntity.ok(keyRotationService.reencryptAll(authentication.getName()));
    }

    @GetMapping("/access-log")
    public ResponseEntity<PageResponse<SensitiveDataAccessLog>> accessLog(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.recent(page, size));
    }
}
