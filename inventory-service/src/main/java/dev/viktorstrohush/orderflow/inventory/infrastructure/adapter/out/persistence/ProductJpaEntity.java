package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
class ProductJpaEntity {

    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private long version;

    protected ProductJpaEntity() {
    }

    String getSku() { return sku; }
    String getName() { return name; }
    BigDecimal getPrice() { return price; }
    int getAvailableQuantity() { return availableQuantity; }
    void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
}
