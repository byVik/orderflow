package dev.viktorstrohush.orderflow.order.application.port.out;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface ProductCatalog {

    /**
     * Current price of each SKU. SKUs that do not exist are not in the map.
     *
     * @throws CatalogUnavailableException if the catalog does not respond or responds with an error
     */
    Map<String, BigDecimal> pricesFor(Set<String> skus);
}
