package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class ProductPersistenceAdapter implements ProductRepository {

    private final SpringDataProductRepository jpa;

    ProductPersistenceAdapter(SpringDataProductRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Product> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findBySkus(Collection<String> skus) {
        return jpa.findBySkuIn(skus).stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findBySkusForUpdate(Collection<String> skus) {
        // Orden estable por SKU para evitar interbloqueos entre reservas concurrentes.
        return jpa.findBySkuInForUpdate(skus).stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void saveAll(List<Product> products) {
        Map<String, ProductJpaEntity> entities = jpa.findBySkuIn(products.stream().map(Product::sku).toList())
                .stream().collect(Collectors.toMap(ProductJpaEntity::getSku, Function.identity()));
        products.forEach(p -> entities.get(p.sku()).setAvailableQuantity(p.availableQuantity()));
    }

    private static Product toDomain(ProductJpaEntity e) {
        return new Product(e.getSku(), e.getName(), e.getPrice(), e.getAvailableQuantity());
    }
}
