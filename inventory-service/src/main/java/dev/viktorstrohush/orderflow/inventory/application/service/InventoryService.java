package dev.viktorstrohush.orderflow.inventory.application.service;

import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.application.port.in.ReserveStockUseCase;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationLog;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InventoryService implements ReserveStockUseCase, ListProductsQuery {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final ReservationLog reservations;
    private final StockEventPublisher events;

    public InventoryService(ProductRepository products, ReservationLog reservations, StockEventPublisher events) {
        this.products = products;
        this.reservations = reservations;
        this.events = events;
    }

    @Override
    @Transactional
    public ReservationResult reserve(ReservationRequest request) {
        Optional<ReservationResult> previous = reservations.find(request.orderId());
        if (previous.isPresent()) {
            log.info("Pedido {} ya procesado; se reenvía el resultado anterior", request.orderId());
            events.publish(previous.get());
            return previous.get();
        }

        List<String> skus = request.items().stream().map(ReservationRequest.Item::sku).toList();
        Map<String, Product> bySku = products.findBySkusForUpdate(skus).stream()
                .collect(Collectors.toMap(Product::sku, Function.identity()));

        ReservationResult result = firstProblem(request, bySku)
                .map(reason -> ReservationResult.rejected(request.orderId(), reason))
                .orElseGet(() -> {
                    request.items().forEach(i -> bySku.get(i.sku()).reserve(i.quantity()));
                    products.saveAll(List.copyOf(bySku.values()));
                    return ReservationResult.reserved(request.orderId());
                });

        reservations.record(result);
        events.publish(result);
        log.info("Reserva del pedido {}: {}", request.orderId(), result.reserved() ? "OK" : result.reason());
        return result;
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
