package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto;

import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty @Valid List<Line> lines) {

    /** The maximum is a domain rule; it is only repeated here to answer 400 as early as possible. */
    public record Line(@NotBlank String sku, @Positive @Max(OrderLine.MAX_QUANTITY) int quantity) {
    }
}
