package dev.viktorstrohush.orderflow.inventory.application;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationLog;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.application.service.InventoryService;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    ProductRepository products;
    @Mock
    ReservationLog reservations;
    @Mock
    StockEventPublisher events;

    InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(products, reservations, events);
    }

    private static ReservationRequest request(UUID orderId, String sku, int qty) {
        return new ReservationRequest(orderId, List.of(new ReservationRequest.Item(sku, qty)));
    }

    @Test
    void reservaCuandoHayStockSuficiente() {
        UUID orderId = UUID.randomUUID();
        Product keyboard = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        when(reservations.find(orderId)).thenReturn(Optional.empty());
        when(products.findBySkusForUpdate(anyCollection())).thenReturn(List.of(keyboard));

        ReservationResult result = service.reserve(request(orderId, "KB-01", 2));

        assertThat(result.reserved()).isTrue();
        assertThat(keyboard.availableQuantity()).isEqualTo(3);
        verify(products).saveAll(List.of(keyboard));
        verify(events).publish(result);
    }

    @Test
    void rechazaSinTocarElStockSiFaltaAlgunProducto() {
        UUID orderId = UUID.randomUUID();
        Product keyboard = new Product("KB-01", "Teclado", BigDecimal.TEN, 1);
        when(reservations.find(orderId)).thenReturn(Optional.empty());
        when(products.findBySkusForUpdate(anyCollection())).thenReturn(List.of(keyboard));

        ReservationResult result = service.reserve(request(orderId, "KB-01", 2));

        assertThat(result.reserved()).isFalse();
        assertThat(result.reason()).contains("Stock insuficiente de KB-01");
        assertThat(keyboard.availableQuantity()).isEqualTo(1);
        verify(products, never()).saveAll(any());
    }

    @Test
    void unMensajeDuplicadoNoVuelveAReservar() {
        UUID orderId = UUID.randomUUID();
        ReservationResult previous = ReservationResult.reserved(orderId);
        when(reservations.find(orderId)).thenReturn(Optional.of(previous));

        ReservationResult result = service.reserve(request(orderId, "KB-01", 2));

        assertThat(result).isEqualTo(previous);
        verify(products, never()).findBySkusForUpdate(anyCollection());
        verify(events).publish(previous);
    }
}
