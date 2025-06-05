package com.hellfire.request;

import com.hellfire.model.PaymentMethods;
import lombok.Data;

@Data
public class ChangePaymentMethodRequest {
    private Long orderId;
    private PaymentMethods paymentMethod;

    // Getters and Setters
}

