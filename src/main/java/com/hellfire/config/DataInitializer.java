package com.hellfire.config;

import com.hellfire.model.RestaurantStatus;
import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.model.UserStatus;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.team.service.PlatformSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Startup tasks:
 * <ul>
 *   <li>backfill status/createdAt on rows that predate those columns,</li>
 *   <li>make sure the platform settings row exists,</li>
 *   <li>seed the first TEAM_ADMIN from TEAM_ADMIN_EMAIL / TEAM_ADMIN_PASSWORD (skipped when unset).</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformSettingsService platformSettingsService;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RestaurantRepository restaurantRepository,
                           PasswordEncoder passwordEncoder,
                           PlatformSettingsService platformSettingsService,
                           @Value("${team.admin.name:Platform Admin}") String adminName,
                           @Value("${team.admin.email:}") String adminEmail,
                           @Value("${team.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.platformSettingsService = platformSettingsService;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        backfillLegacyRows();
        platformSettingsService.get();
        seedTeamAdmin();
    }

    private void backfillLegacyRows() {
        int users = userRepository.backfillMissingStatus(UserStatus.ACTIVE);
        int stamped = userRepository.backfillMissingCreatedAt(LocalDateTime.now());
        int restaurants = restaurantRepository.backfillMissingStatus(RestaurantStatus.ACTIVE);
        if (users + stamped + restaurants > 0) {
            log.info("Backfilled legacy rows: {} user statuses, {} user timestamps, {} restaurant statuses",
                    users, stamped, restaurants);
        }
    }

    private void seedTeamAdmin() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("TEAM_ADMIN_EMAIL / TEAM_ADMIN_PASSWORD not set; skipping team admin seeding");
            return;
        }

        User existing = userRepository.findByEmail(adminEmail);
        if (existing != null) {
            if (existing.getRole() != UserRole.TEAM_ADMIN) {
                log.warn("Seed email {} already belongs to a {} account; not changing it", adminEmail, existing.getRole());
            }
            return;
        }

        User admin = new User();
        admin.setFullName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.TEAM_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
        log.info("Seeded TEAM_ADMIN account {}", adminEmail);
    }
}
