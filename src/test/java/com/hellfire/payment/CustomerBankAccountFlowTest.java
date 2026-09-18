package com.hellfire.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.*;
import com.hellfire.repository.CustomerBankAccountRepository;
import com.hellfire.repository.RefundRepository;
import com.hellfire.repository.SensitiveDataAccessLogRepository;
import com.hellfire.security.SensitiveDataAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Saved customer refund accounts: encryption at rest, masking, access audit, and use in refunds. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerBankAccountFlowTest extends MoneyFlowTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CustomerBankAccountRepository accountRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private SensitiveDataAccessLogRepository accessLogRepository;

    private User customer;
    private User owner;
    private User teamAdmin;
    private User support;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        customer = user("cust@test.local", UserRole.CUSTOMER);
        owner = user("owner@test.local", UserRole.ADMIN);
        teamAdmin = user("teamadmin@test.local", UserRole.TEAM_ADMIN);
        support = user("support@test.local", UserRole.TEAM_CUSTOMER_SUPPORT);
        restaurant = restaurant(owner, true);
        fillCart(customer, food(restaurant, "100.00"), 1);
    }

    @Test
    void customerSavesAccountEncryptedAndTeamAccessIsAudited() throws Exception {
        mockMvc.perform(get("/api/users/me/bank-account").header("Authorization", bearer(customer)))
                .andExpect(status().isNoContent());

        // Needs either account+IFSC or a UPI id.
        mockMvc.perform(put("/api/users/me/bank-account").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("accountHolderName", "Cust Omer"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/users/me/bank-account").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountHolderName", "Cust Omer", "accountNumber", "999988887777",
                                "ifsc", "icic0000123", "bankName", "ICICI", "upiId", "cust@okicici"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masked").value(false))
                .andExpect(jsonPath("$.accountNumber").value("999988887777"))
                .andExpect(jsonPath("$.ifsc").value("ICIC0000123"));

        CustomerBankAccount saved = accountRepository.findByUserId(customer.getId()).orElseThrow();
        String storedNumber = jdbcTemplate.queryForObject(
                "select account_number from customer_bank_accounts where id = ?", String.class, saved.getId());
        String storedUpi = jdbcTemplate.queryForObject(
                "select upi_id from customer_bank_accounts where id = ?", String.class, saved.getId());
        assertTrue(storedNumber.startsWith("enc:v2:"), "account number must be encrypted at rest");
        assertTrue(storedUpi.startsWith("enc:v2:"), "UPI id must be encrypted at rest");
        assertFalse(saved.toString().contains("999988887777"), "toString must not leak the account number");
        assertFalse(saved.toString().contains("cust@okicici"), "toString must not leak the UPI id");

        // Support cannot see it at all; admin sees it unmasked and the reveal is logged.
        mockMvc.perform(get("/api/team/customers/{id}/bank-account", customer.getId()).header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());
        assertEquals(0, accessLogRepository.countBySubjectTypeAndSubjectId(SensitiveDataAuditService.CUSTOMER_BANK_ACCOUNT, customer.getId()));

        mockMvc.perform(get("/api/team/customers/{id}/bank-account", customer.getId()).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("999988887777"));
        assertEquals(1, accessLogRepository.countBySubjectTypeAndSubjectId(SensitiveDataAuditService.CUSTOMER_BANK_ACCOUNT, customer.getId()));

        mockMvc.perform(get("/api/team/security/access-log").header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorEmail").value(teamAdmin.getEmail()))
                .andExpect(jsonPath("$.content[0].subjectType").value("CUSTOMER_BANK_ACCOUNT"));
        mockMvc.perform(get("/api/team/security/access-log").header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/me/bank-account").header("Authorization", bearer(customer)))
                .andExpect(status().isNoContent());
        assertTrue(accountRepository.findByUserId(customer.getId()).isEmpty());
    }

    @Test
    void cashRefundFallsBackToSavedAccountAndCanSaveFromRequest() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "CASH_ON_DELIVERY"))))
                .andExpect(status().isCreated()).andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(put("/api/admin/order/{id}/DELIVERED", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());

        // No saved account and none supplied -> refused.
        mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"cold food\",\"amount\":30}"))
                .andExpect(status().isBadRequest());

        // Supplying details with saveBankAccount stores them for next time.
        MvcResult refund = mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "cold food", "amount", 30, "saveBankAccount", true,
                                "bankAccount", Map.of("beneficiaryName", "Cust Omer", "upiId", "cust@okaxis")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bankAccount.upiId").value("cust@okaxis"))
                .andReturn();
        long refundId = objectMapper.readTree(refund.getResponse().getContentAsString()).get("id").asLong();
        assertEquals("cust@okaxis", accountRepository.findByUserId(customer.getId()).orElseThrow().getUpiId());

        // Close that refund so a second one is allowed, then request again without details: saved account is used.
        mockMvc.perform(put("/api/team/refunds/{id}/reject", refundId).header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\"}"))
                .andExpect(status().isOk());

        MvcResult second = mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"still cold\",\"amount\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bankAccount.accountHolderName").value("Cust Omer"))
                .andExpect(jsonPath("$.bankAccount.upiId").value("cust@okaxis"))
                .andReturn();
        long secondId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asLong();

        String storedUpi = jdbcTemplate.queryForObject("select upi_id from refunds where id = ?", String.class, secondId);
        assertTrue(storedUpi.startsWith("enc:v2:"), "refund destination must be encrypted at rest");
        assertFalse(refundRepository.findById(secondId).orElseThrow().toString().contains("cust@okaxis"));

        // Support sees the destination masked; admin's view is audited.
        mockMvc.perform(get("/api/team/refunds/{id}", secondId).header("Authorization", bearer(support)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankAccount.masked").value(true))
                .andExpect(jsonPath("$.bankAccount.upiId").value(startsWith("cu****@")));
        mockMvc.perform(get("/api/team/refunds/{id}", secondId).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankAccount.masked").value(false));
        assertEquals(1, accessLogRepository.countBySubjectTypeAndSubjectId(SensitiveDataAuditService.REFUND_BANK_ACCOUNT, secondId));
    }

    @Test
    void encryptionStatusAndReencryptionAreAdminOnly() throws Exception {
        mockMvc.perform(put("/api/users/me/bank-account").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("accountHolderName", "Cust", "upiId", "cust@upi"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/team/security/encryption").header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/team/security/encryption").header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentKeyFingerprint").isNotEmpty())
                .andExpect(jsonPath("$.strongKey").value(true))
                .andExpect(jsonPath("$.rowsNotOnCurrentKey").isEmpty());

        // Everything is already on the current key, so re-encryption is a no-op.
        mockMvc.perform(post("/api/team/security/encryption/rotate").header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reencrypted['customer_bank_accounts.upi_id']").value(0));
    }
}
