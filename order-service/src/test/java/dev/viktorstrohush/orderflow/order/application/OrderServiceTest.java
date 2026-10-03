package dev.viktorstrohush.orderflow.order.application;

import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
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
        service = new OrderService(repository, publisher, catalog);
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
    void unClienteNoPuedeVerPedidosDeOtro() {
        Order order = Order.place("otro", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getById(order.id(), "viktor"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void elResultadoDeStockEsIdempotente() {
        Order order = Order.place("viktor", List.of(new OrderLine("KB-01", 1, BigDecimal.TEN)));
        order.confirm();
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        service.apply(order.id(), false, "duplicado");

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository, never()).save(any());
    }
}
