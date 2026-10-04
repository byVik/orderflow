package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import dev.viktorstrohush.orderflow.order.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los tests de rebanada no escanean las clases @Configuration, así que la seguridad real hay que
 * importarla. Sin el @Import se probaría la configuración por defecto de Spring Boot.
 */
@WebMvcTest(controllers = OrderController.class,
        properties = "orderflow.auth.jwt-secret=" + OrderControllerTest.SECRET)
@Import(SecurityConfig.class)
class OrderControllerTest {

    static final String SECRET = "test-secret-for-the-web-slice-0123456789";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    PlaceOrderUseCase placeOrder;
    @MockitoBean
    GetOrdersQuery getOrders;
    @MockitoBean
    CancelOrderUseCase cancelOrder;

    private static Order sampleOrder() {
        return Order.place("viktor", List.of(new OrderLine("KB-01", 2, new BigDecimal("49.90"))));
    }

    private static String token(JwtEncoder encoder, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("viktor")
                .issuedAt(expiresAt.minus(Duration.ofHours(1)))
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void crearPedidoDevuelve201ConLocation() throws Exception {
        Order order = sampleOrder();
        when(placeOrder.place(any())).thenReturn(order);

        mvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("viktor")))
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
    void validaElCuerpoDeLaPeticionEIndicaElCampo() throws Exception {
        mvc.perform(post("/api/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"sku":"KB-01","quantity":101}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("lines[0].quantity")));
    }

    @Test
    void sinTokenDevuelve401DelResourceServer() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
    }

    @Test
    void unTokenFirmadoConLaClaveDelServicioEsValido() throws Exception {
        when(getOrders.listByCustomer("viktor")).thenReturn(List.of());
        String valid = token(jwtEncoder, Instant.now().plus(Duration.ofMinutes(5)));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk());
    }

    @Test
    void unTokenCaducadoDevuelve401() throws Exception {
        String expired = token(jwtEncoder, Instant.now().minus(Duration.ofMinutes(10)));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtraClaveDevuelve401() throws Exception {
        JwtEncoder attacker = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "another-secret-that-is-not-the-service-one".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String forged = token(attacker, Instant.now().plus(Duration.ofMinutes(5)));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
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

        mvc.perform(post("/api/orders/{id}/cancel", id.value()).with(jwt().jwt(j -> j.subject("viktor"))))
                .andExpect(status().isConflict());
    }

    @Test
    void unConflictoDeConcurrenciaAlCancelarDevuelve409YNo500() throws Exception {
        OrderId id = OrderId.newId();
        when(cancelOrder.cancel(eq(id), eq("viktor")))
                .thenThrow(new OptimisticLockingFailureException("otra transacción ganó"));

        mvc.perform(post("/api/orders/{id}/cancel", id.value()).with(jwt().jwt(j -> j.subject("viktor"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("ha cambiado")));
    }

    @Test
    void siElCatalogoNoRespondeDevuelve503() throws Exception {
        when(placeOrder.place(any())).thenThrow(new CatalogUnavailableException(new RuntimeException("timeout")));

        mvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("viktor")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"sku":"KB-01","quantity":1}]}
                                """))
                .andExpect(status().isServiceUnavailable());
    }
}
