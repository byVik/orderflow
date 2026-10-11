package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.OrderResponse;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.PageResponse;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
@RequiredArgsConstructor
class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final GetOrdersQuery getOrders;
    private final CancelOrderUseCase cancelOrder;
    private final OrderWebMapper mapper;

    @PostMapping
    @Operation(summary = "Creates an order (it stays PENDING until inventory reserves the stock)")
    ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        Order order = placeOrder.place(mapper.toCommand(request, jwt.getSubject()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(order.id().value()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    /** The order is fixed (newest first), so the client sends a page number and a size, nothing else. */
    @GetMapping
    @Operation(summary = "Lists one page of the authenticated user's orders, newest first")
    PageResponse<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "10") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        PageResult<Order> orders = getOrders.listByCustomer(jwt.getSubject(), new PageQuery(page, size));
        return PageResponse.from(orders.map(OrderResponse::from));
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
