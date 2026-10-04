package dev.viktorstrohush.orderflow.order.application.port.out;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface ProductCatalog {

    /**
     * Precio actual de cada SKU. Los SKUs que no existen no aparecen en el mapa.
     *
     * @throws CatalogUnavailableException si el catálogo no responde o responde con error
     */
    Map<String, BigDecimal> pricesFor(Set<String> skus);
}
