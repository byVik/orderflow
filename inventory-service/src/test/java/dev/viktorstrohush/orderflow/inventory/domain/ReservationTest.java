package dev.viktorstrohush.orderflow.inventory.domain;

import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final List<ReservationRequest.Item> ITEMS = List.of(new ReservationRequest.Item("KB-01", 2));

    @Test
    void unaReservaConStockSePuedeLiberarUnaVez() {
        Reservation reservation = Reservation.reserved(UUID.randomUUID(), ITEMS);

        reservation.release();

        assertThat(reservation.status()).isEqualTo(Reservation.Status.RELEASED);
        assertThat(reservation.holdsStock()).isFalse();
        assertThatThrownBy(reservation::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unaReservaRechazadaNoTieneNadaQueLiberar() {
        Reservation reservation = Reservation.rejected(UUID.randomUUID(), "Stock insuficiente");

        assertThatThrownBy(reservation::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elResultadoSoloEsReservadoMientrasElStockSigueApartado() {
        Reservation reservation = Reservation.reserved(UUID.randomUUID(), ITEMS);
        assertThat(reservation.result().reserved()).isTrue();

        reservation.release();

        assertThat(reservation.result().reserved()).isFalse();
        assertThat(reservation.result().reason()).contains("cancelación");
    }
}
