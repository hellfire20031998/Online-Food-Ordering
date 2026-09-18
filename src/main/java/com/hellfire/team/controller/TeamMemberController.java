package com.hellfire.team.controller;

import com.hellfire.team.TeamAuthorities;
import com.hellfire.team.dto.CreateTeamMemberRequest;
import com.hellfire.team.dto.TeamMemberDto;
import com.hellfire.team.dto.UpdateTeamMemberRoleRequest;
import com.hellfire.team.service.TeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Team account management. TEAM_ADMIN only. */
@RestController
@RequestMapping("/api/team/members")
@RequiredArgsConstructor
@PreAuthorize(TeamAuthorities.ADMIN_ONLY)
public class TeamMemberController {

    private final TeamMemberService memberService;

    @GetMapping
    public ResponseEntity<List<TeamMemberDto>> list() {
        return ResponseEntity.ok(memberService.list());
    }

    @PostMapping
    public ResponseEntity<TeamMemberDto> create(@Valid @RequestBody CreateTeamMemberRequest request,
                                                Authentication authentication) {
        return new ResponseEntity<>(memberService.create(request, authentication.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<TeamMemberDto> changeRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateTeamMemberRoleRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(memberService.changeRole(id, request.getRole(), authentication.getName()));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<TeamMemberDto> deactivate(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(memberService.deactivate(id, authentication.getName()));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<TeamMemberDto> reactivate(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(memberService.reactivate(id, authentication.getName()));
    }
}
