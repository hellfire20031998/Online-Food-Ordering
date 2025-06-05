package com.hellfire.request;


import com.hellfire.model.Address;
import lombok.Data;

@Data
public class OrderRequest {

    private  Long restaurantId;
    private Address deliveryAddress;
    private String paymentMethod;
}
