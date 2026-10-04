package dev.viktorstrohush.orderflow.inventory.application;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.application.service.InventoryService;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    ReservationRepository reservations;
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

    private Reservation savedReservation() {
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void reservaCuandoHayStockSuficienteYGuardaLasLineas() {
        UUID orderId = UUID.randomUUID();
        Product keyboard = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        when(reservations.find(orderId)).thenReturn(Optional.empty());
        when(products.findBySkusForUpdate(anyCollection())).thenReturn(List.of(keyboard));

        ReservationResult result = service.reserve(request(orderId, "KB-01", 2));

        assertThat(result.reserved()).isTrue();
        assertThat(keyboard.availableQuantity()).isEqualTo(3);
        verify(products).saveAll(List.of(keyboard));
        verify(events).publish(result);
        Reservation saved = savedReservation();
        assertThat(saved.status()).isEqualTo(Reservation.Status.RESERVED);
        assertThat(saved.items()).containsExactly(new ReservationRequest.Item("KB-01", 2));
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
    void unMensajeDuplicadoNoVuelveAReservarYReenviaElResultado() {
        UUID orderId = UUID.randomUUID();
        Product keyboard = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        Reservation previous = Reservation.reserved(orderId, List.of(new ReservationRequest.Item("KB-01", 2)));
        when(products.findBySkusForUpdate(anyCollection())).thenReturn(List.of(keyboard));
        when(reservations.find(orderId)).thenReturn(Optional.of(previous));

        ReservationResult result = service.reserve(request(orderId, "KB-01", 2));

        assertThat(result).isEqualTo(ReservationResult.reserved(orderId));
        assertThat(keyboard.availableQuantity()).isEqualTo(5);
        verify(products, never()).saveAll(any());
        verify(reservations, never()).save(any());
        verify(events).publish(result);
    }

    @Test
    void cancelarDevuelveElStockReservado() {
        UUID orderId = UUID.randomUUID();
        Product keyboard = new Product("KB-01", "Teclado", BigDecimal.TEN, 3);
        Reservation reserved = Reservation.reserved(orderId, List.of(new ReservationRequest.Item("KB-01", 2)));
        when(reservations.find(orderId)).thenReturn(Optional.of(reserved));
        when(products.findBySkusForUpdate(anyCollection())).thenReturn(List.of(keyboard));

        service.release(orderId);

        assertThat(keyboard.availableQuantity()).isEqualTo(5);
        verify(products).saveAll(List.of(keyboard));
        assertThat(savedReservation().status()).isEqualTo(Reservation.Status.RELEASED);
    }

    @Test
    void unaCancelacionDuplicadaNoDevuelveElStockDosVeces() {
        UUID orderId = UUID.randomUUID();
        Reservation released = Reservation.reserved(orderId, List.of(new ReservationRequest.Item("KB-01", 2)));
        released.release();
        when(reservations.find(orderId)).thenReturn(Optional.of(released));

        service.release(orderId);

        verify(products, never()).findBySkusForUpdate(anyCollection());
        verify(products, never()).saveAll(any());
        verify(reservations, never()).save(any());
    }

    @Test
    void siLaCancelacionLlegaAntesQueElPedidoQuedaRegistradaParaNoReservarDespues() {
        UUID orderId = UUID.randomUUID();
        when(reservations.find(orderId)).thenReturn(Optional.empty());

        service.release(orderId);

        Reservation tombstone = savedReservation();
        assertThat(tombstone.status()).isEqualTo(Reservation.Status.REJECTED);
        assertThat(tombstone.result().reserved()).isFalse();
        verify(products, never()).saveAll(any());
    }
}
