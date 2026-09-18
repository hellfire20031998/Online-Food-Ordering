package com.hellfire.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartMergeResponse {

    private CartDto cart;
    /** Guest items that could not be carried over, with the reason. */
    private List<SkippedItem> skipped;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkippedItem {
        private Long foodId;
        private String name;
        private String reason;
    }
}
