package dev.viktorstrohush.orderflow.order.domain;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderError;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import dev.viktorstrohush.orderflow.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static OrderLine line(String sku, int qty, String price) {
        return new OrderLine(sku, qty, new BigDecimal(price));
    }

    @Nested
    class Place {

        @Test
        void creaUnPedidoPendienteYCalculaElTotal() {
            Order order = Order.place("viktor", List.of(line("KB-01", 2, "49.90"), line("MS-01", 1, "19.99")));

            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.total()).isEqualByComparingTo("119.79");
            assertThat(order.id()).isNotNull();
        }

        @Test
        void rechazaPedidosSinLineas() {
            assertThatThrownBy(() -> Order.place("viktor", List.of()))
                    .isInstanceOf(InvalidOrderException.class);
        }

        @Test
        void rechazaSkusDuplicados() {
            assertThatThrownBy(() -> Order.place("viktor", List.of(line("KB-01", 1, "1"), line("KB-01", 2, "1"))))
                    .isInstanceOfSatisfying(InvalidOrderException.class,
                            e -> assertThat(e.code()).isEqualTo(OrderError.DUPLICATE_SKU));
        }

        @Test
        void rechazaCantidadesNoPositivas() {
            assertThatThrownBy(() -> line("KB-01", 0, "1"))
                    .isInstanceOf(InvalidOrderException.class);
        }

        @Test
        void rechazaCantidadesPorEncimaDelMaximoPorLinea() {
            assertThat(line("KB-01", OrderLine.MAX_QUANTITY, "1").quantity()).isEqualTo(100);
            assertThatThrownBy(() -> line("KB-01", OrderLine.MAX_QUANTITY + 1, "1"))
                    .isInstanceOfSatisfying(InvalidOrderException.class, e -> {
                        assertThat(e.code()).isEqualTo(OrderError.QUANTITY_ABOVE_MAX);
                        assertThat(e.params()).containsEntry("sku", "KB-01").containsEntry("max", 100);
                    });
        }
    }

    @Nested
    class Transitions {

        @Test
        void unPedidoPendientePuedeConfirmarse() {
            Order order = Order.place("viktor", List.of(line("KB-01", 1, "10")));
            order.confirm();
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        void unPedidoRechazadoGuardaElMotivo() {
            Order order = Order.place("viktor", List.of(line("KB-01", 1, "10")));
            order.reject("Sin stock de KB-01");
            assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
            assertThat(order.rejectionReason()).isEqualTo("Sin stock de KB-01");
        }

        @Test
        void noSePuedeCancelarUnPedidoConfirmado() {
            Order order = Order.place("viktor", List.of(line("KB-01", 1, "10")));
            order.confirm();
            assertThatThrownBy(order::cancel)
                    .isInstanceOfSatisfying(InvalidOrderStateException.class, e -> {
                        assertThat(e.code()).isEqualTo(OrderError.ORDER_NOT_PENDING);
                        assertThat(e.params()).containsEntry("status", "CONFIRMED");
                    });
        }
    }
}
