package com.hellfire.team.service;

import com.hellfire.exceptions.EmailAlreadyRegisteredException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.model.UserStatus;
import com.hellfire.notification.NotificationService;
import com.hellfire.repository.UserRepository;
import com.hellfire.team.dto.CreateTeamMemberRequest;
import com.hellfire.team.dto.TeamMemberDto;
import com.hellfire.team.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Management of platform team accounts (TEAM_* roles). Guards against locking the platform out:
 * a team admin cannot change or deactivate their own account, and the last active TEAM_ADMIN
 * can be neither demoted nor deactivated.
 */
@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private static final Logger log = LoggerFactory.getLogger(TeamMemberService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TeamMemberDto> list() {
        return userRepository.findByRoleInOrderByCreatedAtDesc(UserRole.TEAM_ROLES).stream()
                .map(TeamMapper::toMemberDto)
                .toList();
    }

    @Transactional
    public TeamMemberDto create(CreateTeamMemberRequest request, String actor) {
        requireTeamRole(request.getRole());
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        log.info("AUDIT team member created: id={} email={} role={} by {}",
                saved.getId(), saved.getEmail(), saved.getRole(), actor);
        notificationService.teamMemberCreated(saved, actor);
        return TeamMapper.toMemberDto(saved);
    }

    @Transactional
    public TeamMemberDto changeRole(Long id, UserRole newRole, String actor) {
        requireTeamRole(newRole);
        User target = requireMember(id);
        requireNotSelf(target, actor, "You cannot change your own role");

        boolean demotingAdmin = target.getRole() == UserRole.TEAM_ADMIN && newRole != UserRole.TEAM_ADMIN;
        if (demotingAdmin && TeamMapper.effectiveStatus(target) == UserStatus.ACTIVE) {
            requireAnotherActiveAdmin();
        }

        UserRole before = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);
        log.info("AUDIT team member role: id={} email={} {} -> {} by {}", id, target.getEmail(), before, newRole, actor);
        return TeamMapper.toMemberDto(target);
    }

    @Transactional
    public TeamMemberDto deactivate(Long id, String actor) {
        User target = requireMember(id);
        requireNotSelf(target, actor, "You cannot deactivate your own account");

        if (target.getRole() == UserRole.TEAM_ADMIN && TeamMapper.effectiveStatus(target) == UserStatus.ACTIVE) {
            requireAnotherActiveAdmin();
        }

        target.setStatus(UserStatus.BLOCKED);
        userRepository.save(target);
        log.info("AUDIT team member deactivated: id={} email={} by {}", id, target.getEmail(), actor);
        return TeamMapper.toMemberDto(target);
    }

    @Transactional
    public TeamMemberDto reactivate(Long id, String actor) {
        User target = requireMember(id);
        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);
        log.info("AUDIT team member reactivated: id={} email={} by {}", id, target.getEmail(), actor);
        return TeamMapper.toMemberDto(target);
    }

    // ---------------------------------------------------------------- guards

    private void requireTeamRole(UserRole role) {
        if (role == null || !role.isTeamRole()) {
            throw new IllegalArgumentException("Role must be one of " + UserRole.TEAM_ROLES);
        }
    }

    private User requireMember(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team member with ID " + id + " not found"));
        if (user.getRole() == null || !user.getRole().isTeamRole()) {
            throw new ResourceNotFoundException("Team member with ID " + id + " not found");
        }
        return user;
    }

    private void requireNotSelf(User target, String actor, String message) {
        if (actor != null && actor.equalsIgnoreCase(target.getEmail())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireAnotherActiveAdmin() {
        long activeAdmins = userRepository.countByRoleAndStatus(UserRole.TEAM_ADMIN, UserStatus.ACTIVE);
        if (activeAdmins <= 1) {
            throw new IllegalArgumentException("At least one active TEAM_ADMIN must remain");
        }
    }
}
