package com.hellfire.team.service;

import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.model.UserStatus;
import com.hellfire.notification.NotificationService;
import com.hellfire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamMemberServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeamMemberService service;

    private User onlyAdmin;
    private User manager;

    @BeforeEach
    void setUp() {
        onlyAdmin = member(1L, "admin@platform.local", UserRole.TEAM_ADMIN);
        manager = member(2L, "manager@platform.local", UserRole.TEAM_MANAGER);
    }

    @Test
    void lastActiveAdminCannotBeDemoted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.countByRoleAndStatus(UserRole.TEAM_ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(1L, UserRole.TEAM_MANAGER, "other@platform.local"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.countByRoleAndStatus(UserRole.TEAM_ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.deactivate(1L, "other@platform.local"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminCanBeDemotedWhenAnotherActiveAdminExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.countByRoleAndStatus(UserRole.TEAM_ADMIN, UserStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(UserRole.TEAM_MANAGER,
                service.changeRole(1L, UserRole.TEAM_MANAGER, "other@platform.local").getRole());
    }

    @Test
    void cannotActOnOwnAccount() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThrows(IllegalArgumentException.class, () -> service.deactivate(2L, "manager@platform.local"));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(2L, UserRole.TEAM_ADMIN, "Manager@Platform.local"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void nonTeamRolesAreRejected() {
        // The role is validated before any lookup, so no repository stubbing is needed.
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(2L, UserRole.CUSTOMER, "admin@platform.local"));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(2L, UserRole.ADMIN, "admin@platform.local"));
    }

    @Test
    void nonTeamUsersAreNotMembers() {
        User customer = member(3L, "c@test.local", UserRole.CUSTOMER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));

        assertThrows(ResourceNotFoundException.class, () -> service.deactivate(3L, "admin@platform.local"));
    }

    @Test
    void deactivatingManagerBlocksAccount() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(UserStatus.BLOCKED, service.deactivate(2L, "admin@platform.local").getStatus());
        verify(userRepository, never()).countByRoleAndStatus(any(), any());
    }

    private static User member(Long id, String email, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName(email);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }
}
