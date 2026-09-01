package com.hellfire.request;

import com.hellfire.model.PaymentMethods;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePaymentMethodRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private PaymentMethods paymentMethod;
}
