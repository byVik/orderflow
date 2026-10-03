package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotBlank String sku, @Positive @Max(100) int quantity) {
    }
}
