package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Adaptador de salida que consulta los precios al catálogo de inventory-service. */
@Component
class InventoryCatalogClient implements ProductCatalog {

    private final RestClient http;

    InventoryCatalogClient(RestClient.Builder builder,
                           @Value("${orderflow.inventory.base-url}") String baseUrl,
                           @Value("${orderflow.inventory.connect-timeout}") Duration connectTimeout,
                           @Value("${orderflow.inventory.read-timeout}") Duration readTimeout) {
        // Sin timeouts, un inventory-service lento dejaría las peticiones de pedidos colgadas.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        this.http = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public Map<String, BigDecimal> pricesFor(Set<String> skus) {
        List<ProductDto> products;
        try {
            products = http.get()
                    .uri(uri -> uri.path("/api/products").queryParam("skus", String.join(",", skus)).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            throw new CatalogUnavailableException(e);
        }
        if (products == null) {
            return Map.of();
        }
        return products.stream().collect(Collectors.toMap(ProductDto::sku, ProductDto::price));
    }

    record ProductDto(String sku, String name, BigDecimal price, int availableQuantity) {
    }
}
