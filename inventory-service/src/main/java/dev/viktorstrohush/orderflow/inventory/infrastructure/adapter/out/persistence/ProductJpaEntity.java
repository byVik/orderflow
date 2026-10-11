package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ProductJpaEntity {

    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** The only field that changes, so the only one with a setter. */
    @Setter(AccessLevel.PACKAGE)
    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private long version;
}
