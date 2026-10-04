package dev.viktorstrohush.orderflow.inventory.domain;

import dev.viktorstrohush.orderflow.inventory.domain.exception.InsufficientStockException;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void reservarDescuentaElStock() {
        Product p = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        p.reserve(3);
        assertThat(p.availableQuantity()).isEqualTo(2);
    }

    @Test
    void noSePuedeReservarMasDeLoDisponible() {
        Product p = new Product("KB-01", "Teclado", BigDecimal.TEN, 2);
        assertThatThrownBy(() -> p.reserve(3)).isInstanceOf(InsufficientStockException.class);
        assertThat(p.availableQuantity()).isEqualTo(2);
    }

    @Test
    void liberarDevuelveLasUnidadesAlStock() {
        Product p = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        p.reserve(3);
        p.release(3);
        assertThat(p.availableQuantity()).isEqualTo(5);
    }

    @Test
    void noSePuedeLiberarUnaCantidadNoPositiva() {
        Product p = new Product("KB-01", "Teclado", BigDecimal.TEN, 5);
        assertThatThrownBy(() -> p.release(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
