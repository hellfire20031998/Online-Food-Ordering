package com.hellfire.response;

import com.hellfire.model.Order;
import lombok.Data;

@Data
public class OrderResponse {

    private Order order;
    private Authorized authorized;
}
