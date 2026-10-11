package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.OrderResponse;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final GetOrdersQuery getOrders;
    private final CancelOrderUseCase cancelOrder;

    OrderController(PlaceOrderUseCase placeOrder, GetOrdersQuery getOrders, CancelOrderUseCase cancelOrder) {
        this.placeOrder = placeOrder;
        this.getOrders = getOrders;
        this.cancelOrder = cancelOrder;
    }

    @PostMapping
    @Operation(summary = "Creates an order (it stays PENDING until inventory reserves the stock)")
    ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        var command = new PlaceOrderCommand(jwt.getSubject(), request.lines().stream()
                .map(l -> new PlaceOrderCommand.Line(l.sku(), l.quantity()))
                .toList());
        Order order = placeOrder.place(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(order.id().value()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping
    @Operation(summary = "Lists the authenticated user's orders")
    List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return getOrders.listByCustomer(jwt.getSubject()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Details of one of the authenticated user's orders")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return OrderResponse.from(getOrders.getById(new OrderId(id), jwt.getSubject()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancels an order that is still PENDING")
    OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return OrderResponse.from(cancelOrder.cancel(new OrderId(id), jwt.getSubject()));
    }
}
