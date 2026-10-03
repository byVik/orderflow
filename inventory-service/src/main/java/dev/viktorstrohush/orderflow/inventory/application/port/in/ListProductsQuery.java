package dev.viktorstrohush.orderflow.inventory.application.port.in;

import dev.viktorstrohush.orderflow.inventory.domain.model.Product;

import java.util.Collection;
import java.util.List;

public interface ListProductsQuery {

    List<Product> listAll();

    List<Product> findBySkus(Collection<String> skus);
}
