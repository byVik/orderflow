package dev.viktorstrohush.orderflow.inventory.application.port.out;

import dev.viktorstrohush.orderflow.inventory.domain.model.Product;

import java.util.Collection;
import java.util.List;

public interface ProductRepository {

    List<Product> findAll();

    List<Product> findBySkus(Collection<String> skus);

    /** Igual que findBySkus pero bloqueando las filas (SELECT ... FOR UPDATE). */
    List<Product> findBySkusForUpdate(Collection<String> skus);

    void saveAll(List<Product> products);
}
