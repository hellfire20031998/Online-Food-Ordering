package com.hellfire.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Items a visitor collected before signing in, and how to combine them with the account's cart. */
@Data
public class CartMergeRequest {

    public enum Strategy {
        /** Add the guest items to whatever the account cart already holds (same restaurant only). */
        MERGE,
        /** Empty the account cart, then add the guest items. */
        REPLACE,
        /** Discard the guest items and keep the account cart. */
        KEEP_SERVER
    }

    @NotNull
    private Strategy strategy;

    @Valid
    private List<GuestItem> items;

    @Data
    public static class GuestItem {
        @NotNull
        private Long foodId;
        @Min(1)
        private int quantity;
        private List<String> ingredients;
    }
}
