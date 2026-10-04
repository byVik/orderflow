package dev.viktorstrohush.orderflow.order.application;

import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.application.service.OrderService;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository repository;
    @Mock
    OrderEventPublisher publisher;
    @Mock
    ProductCatalog catalog;

    OrderService service;

    @BeforeEach
    void setUp() {
        // Sin Spring no hay transacción real: el bloque transaccional se ejecuta tal cual.
        service = new OrderService(repository, publisher, catalog, TransactionOperations.withoutTransaction());
    }

    private static Order pendingOrder(String customer) {
        return Order.place(customer, List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));
    }

    @Test
    void creaElPedidoConLosPreciosDelCatalogoYPublicaElEvento() {
        when(catalog.pricesFor(anySet())).thenReturn(Map.of("KB-01", new BigDecimal("49.90")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.place(new PlaceOrderCommand("viktor", List.of(new PlaceOrderCommand.Line("KB-01", 2))));

        assertThat(order.total()).isEqualByComparingTo("99.80");
        verify(publisher).publishOrderPlaced(order);
    }

    @Test
    void fallaSiElProductoNoExiste() {
        when(catalog.pricesFor(anySet())).thenReturn(Map.of());

        assertThatThrownBy(() -> service.place(
                new PlaceOrderCommand("viktor", List.of(new PlaceOrderCommand.Line("NOPE", 1)))))
                .isInstanceOf(InvalidOrderException.class);
        verify(publisher, never()).publishOrderPlaced(any());
    }

    @Test
    void siElCatalogoNoRespondeNoSeGuardaNiSePublicaNada() {
        when(catalog.pricesFor(anySet())).thenThrow(new CatalogUnavailableException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.place(
                new PlaceOrderCommand("viktor", List.of(new PlaceOrderCommand.Line("KB-01", 1)))))
                .isInstanceOf(CatalogUnavailableException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publishOrderPlaced(any());
    }

    @Test
    void unClienteNoPuedeVerPedidosDeOtro() {
        Order order = pendingOrder("otro");
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getById(order.id(), "viktor"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelarUnPedidoPendientePublicaLaCancelacion() {
        Order order = pendingOrder("viktor");
        when(repository.findById(order.id())).thenReturn(Optional.of(order));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order cancelled = service.cancel(order.id(), "viktor");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(publisher).publishOrderCancelled(order);
    }

    @Test
    void unClienteNoPuedeCancelarPedidosDeOtro() {
        Order order = pendingOrder("otro");
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(order.id(), "viktor"))
                .isInstanceOf(OrderNotFoundException.class);
        verify(publisher, never()).publishOrderCancelled(any());
    }

    @Test
    void elResultadoDeStockEsIdempotente() {
        Order order = pendingOrder("viktor");
        order.confirm();
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        service.apply(order.id(), false, "duplicado");

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository, never()).save(any());
    }

    @Test
    void unResultadoParaUnPedidoCanceladoSeIgnora() {
        Order order = pendingOrder("viktor");
        order.cancel();
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        service.apply(order.id(), true, null);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(repository, never()).save(any());
    }
}
