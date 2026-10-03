package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlaceOrderUseCase placeOrder;
    @MockitoBean
    GetOrdersQuery getOrders;
    @MockitoBean
    CancelOrderUseCase cancelOrder;

    private static Order sampleOrder() {
        return Order.place("viktor", List.of(new OrderLine("KB-01", 2, new BigDecimal("49.90"))));
    }

    @Test
    void crearPedidoDevuelve201ConLocation() throws Exception {
        Order order = sampleOrder();
        when(placeOrder.place(any())).thenReturn(order);

        mvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("viktor"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"sku":"KB-01","quantity":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/" + order.id()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(99.80));
    }

    @Test
    void validaElCuerpoDeLaPeticion() throws Exception {
        mvc.perform(post("/api/orders")
                        .with(jwt()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"sku":"","quantity":0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void pedidoDeOtroClienteDevuelve404() throws Exception {
        Order order = sampleOrder();
        when(getOrders.getById(eq(order.id()), eq("viktor"))).thenThrow(new OrderNotFoundException(order.id()));

        mvc.perform(get("/api/orders/{id}", order.id().value()).with(jwt().jwt(j -> j.subject("viktor"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelarUnPedidoNoPendienteDevuelve409() throws Exception {
        OrderId id = OrderId.newId();
        when(cancelOrder.cancel(eq(id), eq("viktor"))).thenThrow(new InvalidOrderStateException("no"));

        mvc.perform(post("/api/orders/{id}/cancel", id.value())
                        .with(jwt().jwt(j -> j.subject("viktor"))).with(csrf()))
                .andExpect(status().isConflict());
    }
}
