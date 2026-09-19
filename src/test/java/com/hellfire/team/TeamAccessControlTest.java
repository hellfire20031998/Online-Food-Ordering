package com.hellfire.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.config.JwtProvider;
import com.hellfire.model.*;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authorization rules for the platform team console, signup lockdown,
 * blocked accounts and suspended restaurants. Runs on the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeamAccessControlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User customer;
    private User owner;
    private User support;
    private User teamAdmin;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        customer = user("cust@test.local", UserRole.CUSTOMER);
        owner = user("owner@test.local", UserRole.ADMIN);
        support = user("support@test.local", UserRole.TEAM_CUSTOMER_SUPPORT);
        teamAdmin = user("teamadmin@test.local", UserRole.TEAM_ADMIN);

        restaurant = new Restaurant();
        restaurant.setName("Test Kitchen");
        restaurant.setCuisineType("Indian");
        restaurant.setOwner(owner);
        restaurant.setOpen(true);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant = restaurantRepository.save(restaurant);
    }

    // ------------------------------------------------------------ signup lockdown

    @Test
    void signupIgnoresRoleInBodyAndCreatesCustomer() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Sneaky", "email", "sneaky@test.local", "password", "secret123", "role", "TEAM_ADMIN"));

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertEquals(UserRole.CUSTOMER, userRepository.findByEmail("sneaky@test.local").getRole());
    }

    // ------------------------------------------------------------ route-level access

    @Test
    void customerAndOwnerCannotReachTeamConsole() throws Exception {
        mockMvc.perform(get("/api/team/dashboard/summary").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/team/dashboard/summary").header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportRoleCanReadButNotAct() throws Exception {
        mockMvc.perform(get("/api/team/dashboard/summary").header("Authorization", bearer(support)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRestaurants").value(1));

        mockMvc.perform(get("/api/team/restaurants").header("Authorization", bearer(support)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(put("/api/team/restaurants/{id}/suspend", restaurant.getId())
                        .header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/team/customers/{id}/block", customer.getId())
                        .header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/team/members").header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ public listing order

    @Test
    void publicListingShowsOpenRestaurantsFirstThenClosedOnes() throws Exception {
        Restaurant closed = new Restaurant();
        closed.setName("Aardvark Diner"); // alphabetically first, but closed
        closed.setOwner(user("owner2@test.local", UserRole.ADMIN)); // one restaurant per owner
        closed.setOpen(false);
        closed.setStatus(RestaurantStatus.ACTIVE);
        closed.setRegistrationDate(LocalDateTime.now());
        restaurantRepository.save(closed);

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Test Kitchen"))
                .andExpect(jsonPath("$[0].open").value(true))
                .andExpect(jsonPath("$[1].name").value("Aardvark Diner"))
                .andExpect(jsonPath("$[1].open").value(false));
    }

    // ------------------------------------------------------------ restaurant suspension

    @Test
    void suspendedRestaurantDisappearsFromPublicListing() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/api/team/restaurants/{id}/suspend", restaurant.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/team/restaurants/{id}/reactivate", restaurant.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ------------------------------------------------------------ blocked customers

    @Test
    void blockedCustomerCannotSignInOrUseExistingToken() throws Exception {
        String token = bearer(customer);

        mockMvc.perform(put("/api/team/customers/{id}/block", customer.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        String login = objectMapper.writeValueAsString(Map.of("email", customer.getEmail(), "password", "secret123"));
        mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/profile").header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/team/customers/{id}/unblock", customer.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void teamMemberIsNotACustomer() throws Exception {
        mockMvc.perform(put("/api/team/customers/{id}/block", support.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ team members

    @Test
    void teamAdminManagesMembersButCannotLockThemselvesOut() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Ops Manager", "email", "manager@test.local", "password", "secret123", "role", "TEAM_MANAGER"));
        mockMvc.perform(post("/api/team/members").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEAM_MANAGER"));

        String badRole = objectMapper.writeValueAsString(Map.of(
                "fullName", "Not Team", "email", "x@test.local", "password", "secret123", "role", "CUSTOMER"));
        mockMvc.perform(post("/api/team/members").contentType(MediaType.APPLICATION_JSON).content(badRole)
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/team/members/{id}/deactivate", teamAdmin.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ settings

    @Test
    void commissionIsEditableByAdminOnly() throws Exception {
        mockMvc.perform(get("/api/team/settings").header("Authorization", bearer(support)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"));

        String body = objectMapper.writeValueAsString(Map.of("commissionPercentage", 12.5));
        mockMvc.perform(put("/api/team/settings").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/team/settings").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissionPercentage").value(12.5))
                .andExpect(jsonPath("$.updatedBy").value(teamAdmin.getEmail()));
    }

    // ------------------------------------------------------------ helpers

    private User user(String email, UserRole role) {
        User u = new User();
        u.setFullName(email.substring(0, email.indexOf('@')));
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("secret123"));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private String bearer(User u) {
        return "Bearer " + jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, AuthorityUtils.createAuthorityList(u.getRole().name())));
    }
}
