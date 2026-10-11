package dev.viktorstrohush.orderflow.inventory.application.service;

import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReleaseStockUseCase;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReserveStockUseCase;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.domain.model.RejectionReason;
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
        // Locks first: two deliveries of the same order run one after the other, and the second
        // already finds the first one's reservation when it checks idempotency.
        Map<String, Product> bySku = lockBySku(request.items().stream().map(ReservationRequest.Item::sku).toList());

        Optional<Reservation> previous = reservations.find(request.orderId());
        if (previous.isPresent()) {
            ReservationResult result = previous.get().result();
            log.info("Order {} already processed; resending the previous result", request.orderId());
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
        log.info("Reservation for order {}: {}", request.orderId(), result.reserved() ? "OK" : result.reason());
        return result;
    }

    @Override
    @Transactional
    public void release(UUID orderId) {
        Optional<Reservation> found = reservations.find(orderId);
        if (found.isEmpty()) {
            // OrderCancelled and OrderPlaced travel on different topics and Kafka guarantees no order
            // between them. A record is left so that OrderPlaced, when it arrives, reserves nothing.
            reservations.save(Reservation.rejected(orderId, RejectionReason.CANCELLED_BEFORE_RESERVING));
            log.info("Order {} cancelled before its reservation was processed", orderId);
            return;
        }

        Reservation reservation = found.get();
        if (!reservation.holdsStock()) {
            log.info("Order {} cancelled: nothing to release ({})", orderId, reservation.status());
            return;
        }

        Map<String, Product> bySku = lockBySku(reservation.items().stream().map(ReservationRequest.Item::sku).toList());
        reservation.items().forEach(i -> bySku.get(i.sku()).release(i.quantity()));
        products.saveAll(List.copyOf(bySku.values()));
        reservation.release();
        reservations.save(reservation);
        log.info("Stock for order {} released", orderId);
    }

    private Map<String, Product> lockBySku(List<String> skus) {
        return products.findBySkusForUpdate(skus).stream()
                .collect(Collectors.toMap(Product::sku, Function.identity()));
    }

    /** The reason is a code; the detail (which product, how much there was) stays in the log. */
    private static Optional<String> firstProblem(ReservationRequest request, Map<String, Product> bySku) {
        for (ReservationRequest.Item item : request.items()) {
            Product product = bySku.get(item.sku());
            if (product == null) {
                log.info("Order {}: unknown product {}", request.orderId(), item.sku());
                return Optional.of(RejectionReason.UNKNOWN_PRODUCT);
            }
            if (!product.canReserve(item.quantity())) {
                log.info("Order {}: insufficient stock of {} (requested {}, available {})", request.orderId(),
                        item.sku(), item.quantity(), product.availableQuantity());
                return Optional.of(RejectionReason.INSUFFICIENT_STOCK);
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
