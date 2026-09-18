package com.hellfire.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellfire.model.*;
import com.hellfire.payment.gateway.*;
import com.hellfire.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Online checkout against a mocked gateway: intent creation, client-side confirm, webhook,
 * visibility to the restaurant, cancellation with automatic refund through the gateway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnlinePaymentFlowTest extends MoneyFlowTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRepository paymentRepository;

    @MockitoBean private PaymentGateway gateway;

    private User customer;
    private User owner;
    private User teamAdmin;
    private Restaurant restaurant;
    private Food food;

    @BeforeEach
    void setUp() {
        customer = user("cust@test.local", UserRole.CUSTOMER);
        owner = user("owner@test.local", UserRole.ADMIN);
        teamAdmin = user("teamadmin@test.local", UserRole.TEAM_ADMIN);
        restaurant = restaurant(owner, true);
        food = food(restaurant, "250.00");
        fillCart(customer, food, 2);

        when(gateway.enabled()).thenReturn(true);
        when(gateway.provider()).thenReturn(PaymentProvider.STRIPE);
        when(gateway.publishableKey()).thenReturn("pk_test_123");
        when(gateway.createIntent(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new GatewayIntent("pi_" + p.getId(), "pi_" + p.getId() + "_secret");
        });
    }

    @Test
    void checkoutConfirmAndVisibility() throws Exception {
        mockMvc.perform(get("/api/payments/config").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlinePaymentsEnabled").value(true))
                .andExpect(jsonPath("$.provider").value("STRIPE"))
                .andExpect(jsonPath("$.publishableKey").value("pk_test_123"));

        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "CREDIT_CARD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.payment.provider").value("STRIPE"))
                .andExpect(jsonPath("$.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.payment.amount").value(500.0))
                .andReturn();
        JsonNode order = objectMapper.readTree(created.getResponse().getContentAsString());
        long orderId = order.get("id").asLong();
        long paymentId = order.get("payment").get("id").asLong();
        String intentId = order.get("payment").get("providerPaymentId").asText();
        assertEquals("pi_" + paymentId + "_secret", order.get("payment").get("clientSecret").asText());
        assertEquals(1, cartSize(customer), "the cart stays until the payment is confirmed");

        // Unpaid orders are hidden from the restaurant...
        mockMvc.perform(get("/api/admin/order/restaurant/{rid}", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        // ...and cannot be advanced by it.
        mockMvc.perform(put("/api/admin/order/{id}/OUT_FOR_DELIVERY", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/order/{id}/PAYMENT_PENDING", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest());

        // The customer can fetch the secret again to resume checkout; nobody else can.
        mockMvc.perform(get("/api/payments/order/{id}", orderId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_" + paymentId + "_secret"));
        mockMvc.perform(get("/api/payments/order/{id}", orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());

        // Browser reports success -> server verifies with the gateway.
        when(gateway.fetchIntent(intentId)).thenReturn(new GatewayIntentStatus(GatewayIntentStatus.State.PAID, null));
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
        assertEquals(0, cartSize(customer), "cart is cleared once paid");

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(OrderStatus.PENDING, payment.getOrder().getOrderStatus());
        assertEquals(0, new BigDecimal("50.00").compareTo(payment.getCommissionAmount()));

        mockMvc.perform(get("/api/admin/order/restaurant/{rid}", restaurant.getId()).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].payment.status").value("PAID"));

        // ---- customer cancels the paid order: automatic gateway refund request, approved by the team
        mockMvc.perform(delete("/api/order/{id}", orderId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        MvcResult refunds = mockMvc.perform(get("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].method").value("GATEWAY"))
                .andExpect(jsonPath("$[0].amount").value(500.0))
                .andReturn();
        long refundId = objectMapper.readTree(refunds.getResponse().getContentAsString()).get(0).get("id").asLong();

        when(gateway.refund(eq(intentId), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(new GatewayRefundResult("re_1", true));
        mockMvc.perform(put("/api/team/refunds/{id}/approve", refundId).header("Authorization", bearer(teamAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.providerRefundId").value("re_1"));
        verify(gateway).refund(eq(intentId), eq(new BigDecimal("500.00")), anyString(), eq("refund-" + refundId));

        assertEquals(PaymentStatus.REFUNDED, paymentRepository.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void webhookSettlesPaymentWithoutAuthentication() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "UPI"))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode order = objectMapper.readTree(created.getResponse().getContentAsString());
        String intentId = order.get("payment").get("providerPaymentId").asText();
        long paymentId = order.get("payment").get("id").asLong();

        when(gateway.parseWebhook(eq("raw-stripe-payload"), eq("t=1,v1=sig")))
                .thenReturn(new GatewayWebhookEvent(GatewayWebhookEvent.Type.PAYMENT_SUCCEEDED, intentId, null, null));

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .header("Stripe-Signature", "t=1,v1=sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("raw-stripe-payload"))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.PENDING, payment.getOrder().getOrderStatus());
    }

    @Test
    void failedPaymentMarksOrderAndCancellingReleasesIntent() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/order").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderBody(restaurant.getId(), "DEBIT_CARD"))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode order = objectMapper.readTree(created.getResponse().getContentAsString());
        long orderId = order.get("id").asLong();
        long paymentId = order.get("payment").get("id").asLong();
        String intentId = order.get("payment").get("providerPaymentId").asText();

        when(gateway.fetchIntent(intentId))
                .thenReturn(new GatewayIntentStatus(GatewayIntentStatus.State.FAILED, "Card declined"));
        mockMvc.perform(post("/api/payments/{id}/confirm", paymentId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Card declined"));
        assertEquals(OrderStatus.PAYMENT_FAILED, paymentRepository.findById(paymentId).orElseThrow().getOrder().getOrderStatus());

        mockMvc.perform(delete("/api/order/{id}", orderId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
        assertEquals(PaymentStatus.CANCELLED, paymentRepository.findById(paymentId).orElseThrow().getStatus());

        // No money was taken, so no refund appears.
        mockMvc.perform(get("/api/orders/{id}/refunds", orderId).header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
