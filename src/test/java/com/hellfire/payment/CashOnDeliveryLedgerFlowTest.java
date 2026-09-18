package com.hellfire.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.*;
import com.hellfire.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cash on delivery end to end with no gateway configured: order, delivery settles the payment with
 * commission, a customer refund by bank transfer, then a restaurant payout and the owner's view.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CashOnDeliveryLedgerFlowTest extends MoneyFlowTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRepository paymentRepository;

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
    void onlinePaymentIsRefusedWhenNoGatewayIsConfigured() throws Exception {
        mockMvc.perform(get("/api/payments/config").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePaymentsEnabled").value(false))
                .andExpect(jsonPath("$.currency").value("INR"));

        mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "UPI"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashOrderIsSettledOnDeliveryThenRefundedAndPaidOut() throws Exception {
        // ---- order
        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "CASH_ON_DELIVERY"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.payment.provider").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.payment.clientSecret").doesNotExist())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertEquals(0, cartSize(customer), "cash orders empty the cart immediately");

        // Nothing to refund before delivery.
        mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"changed my mind\"}"))
                .andExpect(status().isBadRequest());

        // ---- delivery settles the cash payment with the commission snapshot
        mockMvc.perform(put("/api/admin/order/{id}/DELIVERED", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.status").value("PAID"))
                .andExpect(jsonPath("$.payment.commissionPercentage").value(10.0))
                .andExpect(jsonPath("$.payment.commissionAmount").value(10.0));

        // ---- customer refund by bank transfer: bank details are mandatory
        mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"item missing\",\"amount\":40}"))
                .andExpect(status().isBadRequest());

        MvcResult refundRes = mockMvc.perform(post("/api/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "item missing", "amount", 40,
                                "bankAccount", Map.of("beneficiaryName", "Cust Omer", "accountNumber", "999988887777", "ifsc", "icic0000123")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.bankAccount.ifsc").value("ICIC0000123"))
                .andExpect(jsonPath("$.bankAccount.accountNumber").value("999988887777"))
                .andReturn();
        long refundId = objectMapper.readTree(refundRes.getResponse().getContentAsString()).get("id").asLong();

        // A second open refund is refused.
        mockMvc.perform(post("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "again",
                                "bankAccount", Map.of("beneficiaryName", "x", "upiId", "x@upi")))))
                .andExpect(status().isBadRequest());

        // ---- support reads (masked) but cannot decide
        mockMvc.perform(get("/api/team/refunds/{id}", refundId).header("Authorization", bearer(support)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankAccount.masked").value(true))
                .andExpect(jsonPath("$.bankAccount.accountNumber").value(startsWith("****")));
        mockMvc.perform(put("/api/team/refunds/{id}/approve", refundId).header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/team/payouts").header("Authorization", bearer(support)))
                .andExpect(status().isForbidden());

        // ---- admin approves, completes with the transfer reference
        mockMvc.perform(put("/api/team/refunds/{id}/approve", refundId).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.bankAccount.masked").value(false));

        mockMvc.perform(put("/api/team/refunds/{id}/complete", refundId).header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/team/refunds/{id}/complete", refundId).header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceNumber\":\"NEFT123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.referenceNumber").value("NEFT123"));

        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        assertEquals(0, new BigDecimal("40.00").compareTo(payment.getRefundedAmount()));
        assertEquals(0, new BigDecimal("54.00").compareTo(payment.getNetToRestaurant()));

        // ---- owner sees unsettled earnings before any payout
        mockMvc.perform(get("/api/admin/restaurants/{rid}/earnings", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsettledPayments").value(1))
                .andExpect(jsonPath("$.unsettledGross").value(60.0))
                .andExpect(jsonPath("$.unsettledCommission").value(6.0))
                .andExpect(jsonPath("$.unsettledNet").value(54.0))
                .andExpect(jsonPath("$.paidOut").value(0));

        // ---- payout generation for today
        String today = LocalDate.now().toString();
        MvcResult generated = mockMvc.perform(post("/api/team/payouts/generate").header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + today + "\",\"to\":\"" + today + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].paymentCount").value(1))
                .andExpect(jsonPath("$[0].grossAmount").value(100.0))
                .andExpect(jsonPath("$[0].refundedAmount").value(40.0))
                .andExpect(jsonPath("$[0].commissionAmount").value(6.0))
                .andExpect(jsonPath("$[0].netAmount").value(54.0))
                .andExpect(jsonPath("$[0].bankAccount.accountNumber").value("123456789012"))
                .andReturn();
        JsonNode payoutJson = objectMapper.readTree(generated.getResponse().getContentAsString()).get(0);
        long payoutId = payoutJson.get("id").asLong();

        // Running it again finds nothing new.
        mockMvc.perform(post("/api/team/payouts/generate").header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + today + "\",\"to\":\"" + today + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/team/payouts/{id}", payoutId).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.payments[0].orderId").value(orderId));

        mockMvc.perform(get("/api/admin/restaurants/{rid}/earnings", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.unsettledPayments").value(0))
                .andExpect(jsonPath("$.pendingPayouts").value(54.0));

        // ---- mark paid requires a reference; then the owner sees it
        mockMvc.perform(put("/api/team/payouts/{id}/mark-paid", payoutId).header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"referenceNumber\":\" \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/team/payouts/{id}/mark-paid", payoutId).header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"referenceNumber\":\"IMPS777\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidBy").value(teamAdmin.getEmail()));

        mockMvc.perform(get("/api/admin/restaurants/{rid}/payouts", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referenceNumber").value("IMPS777"))
                .andExpect(jsonPath("$[0].bankAccount.masked").value(false));

        mockMvc.perform(get("/api/admin/restaurants/{rid}/earnings", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.pendingPayouts").value(0))
                .andExpect(jsonPath("$.paidOut").value(54.0));

        // A different owner cannot peek.
        User otherOwner = user("other@test.local", UserRole.ADMIN);
        mockMvc.perform(get("/api/admin/restaurants/{rid}/payouts", restaurant.getId()).header("Authorization", bearer(otherOwner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellingAPendingPayoutReleasesItsPayments() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "CASH_ON_DELIVERY"))))
                .andExpect(status().isCreated()).andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(put("/api/admin/order/{id}/COMPLETED", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());

        String today = LocalDate.now().toString();
        MvcResult generated = mockMvc.perform(post("/api/team/payouts/generate").header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + today + "\",\"to\":\"" + today + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].netAmount").value(90.0))
                .andReturn();
        long payoutId = objectMapper.readTree(generated.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(put("/api/team/payouts/{id}/cancel", payoutId).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertNull(paymentRepository.findByOrderId(orderId).orElseThrow().getPayout());

        mockMvc.perform(post("/api/team/payouts/generate").header("Authorization", bearer(teamAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + today + "\",\"to\":\"" + today + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
