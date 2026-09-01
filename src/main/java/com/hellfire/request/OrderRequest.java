package com.hellfire.request;

import com.hellfire.model.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull
    private Long restaurantId;

    @NotNull
    private Address deliveryAddress;

    @NotBlank
    private String paymentMethod;
}
