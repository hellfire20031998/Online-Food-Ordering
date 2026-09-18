package com.hellfire.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Inclusive period; every settled, not-yet-paid-out payment with paidAt inside it is included. */
@Data
public class GeneratePayoutsRequest {

    @NotNull
    private LocalDate from;

    @NotNull
    private LocalDate to;
}
