package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Outbound adapter that asks the inventory-service catalog for the prices. */
@Component
@RequiredArgsConstructor
class InventoryCatalogClient implements ProductCatalog {

    /** Name of the retry and of the circuit breaker configured under resilience4j.* in application.yml. */
    static final String INVENTORY = "inventory";

    private final RestClient http;

    /**
     * The two annotations work like @Transactional, through a proxy. Retry is the outer one: each
     * attempt goes through the circuit breaker and counts for it, and with the circuit open the
     * attempt fails at once. When nothing else can be done, the fallback runs.
     */
    @Override
    @Retry(name = INVENTORY, fallbackMethod = "unavailable")
    @CircuitBreaker(name = INVENTORY)
    public Map<String, BigDecimal> pricesFor(Set<String> skus) {
        List<ProductDto> products = http.get()
                .uri(uri -> uri.path("/api/products").queryParam("skus", String.join(",", skus)).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (products == null) {
            return Map.of();
        }
        return products.stream().collect(Collectors.toMap(ProductDto::sku, ProductDto::price));
    }

    /**
     * The port has a single exception: whoever places the order does not care whether the attempts
     * ran out, the request was rejected or the circuit is open. The cause stays inside for the log.
     */
    @SuppressWarnings("unused") // called by Resilience4j, by name
    private Map<String, BigDecimal> unavailable(Set<String> skus, Exception cause) {
        throw new CatalogUnavailableException(cause);
    }

    /** Its own copy of the contract: only the fields this service reads, not inventory-service's class. */
    record ProductDto(String sku, BigDecimal price) {
    }
}
