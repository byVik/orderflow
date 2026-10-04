package dev.viktorstrohush.orderflow.inventory.application.service;

import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReleaseStockUseCase;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReserveStockUseCase;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InventoryService implements ReserveStockUseCase, ReleaseStockUseCase, ListProductsQuery {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    static final String CANCELLED_BEFORE_RESERVING = "Pedido cancelado antes de reservar el stock";

    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final StockEventPublisher events;

    public InventoryService(ProductRepository products, ReservationRepository reservations, StockEventPublisher events) {
        this.products = products;
        this.reservations = reservations;
        this.events = events;
    }

    @Override
    @Transactional
    public ReservationResult reserve(ReservationRequest request) {
        // Primero los bloqueos: dos entregas del mismo pedido quedan en serie, y la segunda ya
        // encuentra la reserva de la primera al comprobar la idempotencia.
        Map<String, Product> bySku = lockBySku(request.items().stream().map(ReservationRequest.Item::sku).toList());

        Optional<Reservation> previous = reservations.find(request.orderId());
        if (previous.isPresent()) {
            ReservationResult result = previous.get().result();
            log.info("Pedido {} ya procesado; se reenvía el resultado anterior", request.orderId());
            events.publish(result);
            return result;
        }

        Reservation reservation = firstProblem(request, bySku)
                .map(reason -> Reservation.rejected(request.orderId(), reason))
                .orElseGet(() -> {
                    request.items().forEach(i -> bySku.get(i.sku()).reserve(i.quantity()));
                    products.saveAll(List.copyOf(bySku.values()));
                    return Reservation.reserved(request.orderId(), request.items());
                });

        reservations.save(reservation);
        ReservationResult result = reservation.result();
        events.publish(result);
        log.info("Reserva del pedido {}: {}", request.orderId(), result.reserved() ? "OK" : result.reason());
        return result;
    }

    @Override
    @Transactional
    public void release(UUID orderId) {
        Optional<Reservation> found = reservations.find(orderId);
        if (found.isEmpty()) {
            // OrderCancelled y OrderPlaced viajan por topics distintos y Kafka no garantiza orden
            // entre ellos. Se deja constancia para que ese OrderPlaced, cuando llegue, no reserve nada.
            reservations.save(Reservation.rejected(orderId, CANCELLED_BEFORE_RESERVING));
            log.info("Pedido {} cancelado antes de procesar su reserva", orderId);
            return;
        }

        Reservation reservation = found.get();
        if (!reservation.holdsStock()) {
            log.info("Pedido {} cancelado: nada que liberar ({})", orderId, reservation.status());
            return;
        }

        Map<String, Product> bySku = lockBySku(reservation.items().stream().map(ReservationRequest.Item::sku).toList());
        reservation.items().forEach(i -> bySku.get(i.sku()).release(i.quantity()));
        products.saveAll(List.copyOf(bySku.values()));
        reservation.release();
        reservations.save(reservation);
        log.info("Stock del pedido {} liberado", orderId);
    }

    private Map<String, Product> lockBySku(List<String> skus) {
        return products.findBySkusForUpdate(skus).stream()
                .collect(Collectors.toMap(Product::sku, Function.identity()));
    }

    private static Optional<String> firstProblem(ReservationRequest request, Map<String, Product> bySku) {
        for (ReservationRequest.Item item : request.items()) {
            Product product = bySku.get(item.sku());
            if (product == null) {
                return Optional.of("Producto desconocido: " + item.sku());
            }
            if (!product.canReserve(item.quantity())) {
                return Optional.of("Stock insuficiente de " + item.sku()
                        + " (disponible " + product.availableQuantity() + ")");
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> listAll() {
        return products.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findBySkus(Collection<String> skus) {
        return products.findBySkus(skus);
    }
}
