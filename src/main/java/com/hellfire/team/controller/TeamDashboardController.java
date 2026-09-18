package com.hellfire.team.controller;

import com.hellfire.team.dto.DashboardSummaryDto;
import com.hellfire.team.service.TeamDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only overview for every platform team role. */
@RestController
@RequestMapping("/api/team/dashboard")
@RequiredArgsConstructor
public class TeamDashboardController {

    private final TeamDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }
}
