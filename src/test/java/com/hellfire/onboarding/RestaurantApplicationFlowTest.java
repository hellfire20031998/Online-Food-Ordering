package com.hellfire.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.config.JwtProvider;
import com.hellfire.email.EmailMessage;
import com.hellfire.email.EmailSender;
import com.hellfire.model.*;
import com.hellfire.repository.RestaurantApplicationRepository;
import com.hellfire.repository.RestaurantBankAccountRepository;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Restaurant owner onboarding end to end: apply, review, approve/reject, payout account, emails. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestaurantApplicationFlowTest {

    private static final String ACCOUNT_NUMBER = "123456789012";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private RestaurantApplicationRepository applicationRepository;
    @Autowired private RestaurantBankAccountRepository bankAccountRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EmailSender emailSender;

    private User customer;
    private User customerSupport;
    private User restaurantSupport;
    private User teamAdmin;

    @BeforeEach
    void setUp() {
        customer = user("applicant@test.local", UserRole.CUSTOMER);
        customerSupport = user("csupport@test.local", UserRole.TEAM_CUSTOMER_SUPPORT);
        restaurantSupport = user("rsupport@test.local", UserRole.TEAM_RESTAURANT_SUPPORT);
        teamAdmin = user("teamadmin@test.local", UserRole.TEAM_ADMIN);
    }

    @Test
    void applicantSubmitsAndSeesMaskedBankDetails() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurant-applications")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applicationBody("Spice Route"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.bankAccount.masked").value(true))
                .andExpect(jsonPath("$.bankAccount.accountNumber").value("********9012"))
                .andExpect(jsonPath("$.bankAccount.ifsc").value("HDFC0001234"))
                .andReturn();

        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Stored encrypted, never as plaintext.
        String stored = jdbcTemplate.queryForObject(
                "select bank_account_number from restaurant_applications where id = ?", String.class, id);
        assertTrue(stored.startsWith("enc:"), "account number must be encrypted at rest");
        assertEquals(ACCOUNT_NUMBER, applicationRepository.findById(id).orElseThrow().getBankAccountNumber());

        // Confirmation email dispatched (asynchronously).
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(3000)).send(captor.capture());
        assertEquals(customer.getEmail(), captor.getValue().to());

        mockMvc.perform(get("/api/restaurant-applications/me").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantName").value("Spice Route"));
    }

    @Test
    void duplicatePendingApplicationAndTeamApplicantsAreRejected() throws Exception {
        submit(customer, "First");

        mockMvc.perform(post("/api/restaurant-applications")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applicationBody("Second"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/restaurant-applications")
                        .header("Authorization", bearer(restaurantSupport))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applicationBody("Team Kitchen"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBankDetailsFailValidation() throws Exception {
        Map<String, Object> body = applicationBody("Bad Bank");
        @SuppressWarnings("unchecked")
        Map<String, Object> bank = (Map<String, Object>) body.get("bankAccount");
        bank.put("ifsc", "NOT-AN-IFSC");

        mockMvc.perform(post("/api/restaurant-applications")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approvalCreatesRestaurantPromotesOwnerAndCopiesBankAccount() throws Exception {
        long id = submit(customer, "Spice Route");
        reset(emailSender);

        // Customer support may read but not approve.
        mockMvc.perform(put("/api/team/restaurant-applications/{id}/approve", id)
                        .header("Authorization", bearer(customerSupport)))
                .andExpect(status().isForbidden());

        // Restaurant support approves, but sees bank details masked.
        mockMvc.perform(put("/api/team/restaurant-applications/{id}/approve", id)
                        .header("Authorization", bearer(restaurantSupport)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(restaurantSupport.getEmail()))
                .andExpect(jsonPath("$.bankAccount.masked").value(true))
                .andExpect(jsonPath("$.bankAccount.accountNumber").value(startsWith("****")));

        User promoted = userRepository.findById(customer.getId()).orElseThrow();
        assertEquals(UserRole.ADMIN, promoted.getRole());

        Restaurant restaurant = restaurantRepository.findByOwnerId(customer.getId());
        assertNotNull(restaurant);
        assertEquals("Spice Route", restaurant.getName());
        assertEquals(RestaurantStatus.ACTIVE, restaurant.getStatus());
        assertFalse(restaurant.isOpen(), "owner opens the restaurant once the menu is ready");
        assertEquals("Pune", restaurant.getAddress().getCity());

        RestaurantBankAccount bank = bankAccountRepository.findByRestaurantId(restaurant.getId()).orElseThrow();
        assertEquals(ACCOUNT_NUMBER, bank.getAccountNumber());
        assertEquals("HDFC0001234", bank.getIfsc());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(3000)).send(captor.capture());
        assertTrue(captor.getValue().subject().contains("approved"));

        // Team admin sees the unmasked account on the application and on the restaurant.
        mockMvc.perform(get("/api/team/restaurant-applications/{id}", id).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankAccount.masked").value(false))
                .andExpect(jsonPath("$.bankAccount.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.restaurantId").value(restaurant.getId()));

        mockMvc.perform(get("/api/team/restaurants/{id}/bank-account", restaurant.getId())
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));

        mockMvc.perform(get("/api/team/restaurants/{id}/bank-account", restaurant.getId())
                        .header("Authorization", bearer(restaurantSupport)))
                .andExpect(status().isForbidden());

        // Approving twice is a client error.
        mockMvc.perform(put("/api/team/restaurant-applications/{id}/approve", id)
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isBadRequest());

        // The new owner (with a fresh ADMIN token) manages their own payout account.
        String ownerToken = bearer(promoted);
        mockMvc.perform(get("/api/admin/restaurants/{rid}/bank-account", restaurant.getId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masked").value(false))
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));

        Map<String, Object> update = bankBody();
        update.put("accountNumber", "999988887777");
        update.put("upiId", "spice@okhdfc");
        mockMvc.perform(put("/api/admin/restaurants/{rid}/bank-account", restaurant.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("999988887777"))
                .andExpect(jsonPath("$.upiId").value("spice@okhdfc"));
    }

    @Test
    void rejectionRequiresReasonAndAllowsReapplying() throws Exception {
        long id = submit(customer, "Spice Route");

        mockMvc.perform(put("/api/team/restaurant-applications/{id}/reject", id)
                        .header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/team/restaurant-applications/{id}/reject", id)
                        .header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Missing FSSAI licence\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Missing FSSAI licence"));

        assertEquals(UserRole.CUSTOMER, userRepository.findById(customer.getId()).orElseThrow().getRole());
        assertNull(restaurantRepository.findByOwnerId(customer.getId()));

        // Can apply again after a rejection.
        submit(customer, "Spice Route v2");

        mockMvc.perform(get("/api/team/restaurant-applications").param("status", "PENDING")
                        .header("Authorization", bearer(customerSupport)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].restaurantName").value("Spice Route v2"));
    }

    @Test
    void applicantCanWithdrawOnlyOwnPendingApplication() throws Exception {
        long id = submit(customer, "Spice Route");
        User other = user("other@test.local", UserRole.CUSTOMER);

        mockMvc.perform(put("/api/restaurant-applications/{id}/withdraw", id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/restaurant-applications/{id}/withdraw", id).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mockMvc.perform(put("/api/team/restaurant-applications/{id}/approve", id)
                        .header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customersCannotReachTheReviewQueue() throws Exception {
        mockMvc.perform(get("/api/team/restaurant-applications").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ helpers

    private long submit(User applicant, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurant-applications")
                        .header("Authorization", bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applicationBody(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static Map<String, Object> applicationBody(String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("restaurantName", name);
        body.put("description", "Home-style curries");
        body.put("cuisineType", "Indian");
        body.put("openingHours", "Mon-Sun 10:00-22:00");
        body.put("applicantPhone", "9999999999");
        body.put("address", Map.of("streetAddress", "1 MG Road", "city", "Pune", "state", "MH", "pincode", "411001", "country", "India"));
        body.put("contact", Map.of("email", "hello@spice.local", "mobile", "9999999999"));
        body.put("images", List.of("https://img.local/1.jpg"));
        body.put("bankAccount", bankBody());
        return body;
    }

    private static Map<String, Object> bankBody() {
        Map<String, Object> bank = new LinkedHashMap<>();
        bank.put("accountHolderName", "Spice Route Foods");
        bank.put("accountNumber", ACCOUNT_NUMBER);
        bank.put("ifsc", "hdfc0001234");
        bank.put("bankName", "HDFC Bank");
        return bank;
    }

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
