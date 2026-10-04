package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto;

import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty @Valid List<Line> lines) {

    /** El máximo es una regla del dominio; aquí solo se repite para responder 400 cuanto antes. */
    public record Line(@NotBlank String sku, @Positive @Max(OrderLine.MAX_QUANTITY) int quantity) {
    }
}
