package com.hellfire.team.controller;

import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.PlatformSettingsDto;
import com.hellfire.team.dto.UpdatePlatformSettingsRequest;
import com.hellfire.team.service.PlatformSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team/settings")
@RequiredArgsConstructor
public class PlatformSettingsController {

    private final PlatformSettingsService settingsService;

    @GetMapping
    public ResponseEntity<PlatformSettingsDto> get() {
        return ResponseEntity.ok(settingsService.getDto());
    }

    @PutMapping
    @PreAuthorize(TeamAuthorities.ADMIN_OR_MANAGER)
    public ResponseEntity<PlatformSettingsDto> update(@Valid @RequestBody UpdatePlatformSettingsRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(
                settingsService.updateCommission(request.getCommissionPercentage(), authentication.getName()));
    }
}
