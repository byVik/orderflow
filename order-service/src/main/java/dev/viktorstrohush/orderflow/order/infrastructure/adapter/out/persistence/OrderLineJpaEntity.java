package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The only field with a setter: the order assigns itself in {@code addLine}. */
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderJpaEntity order;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    OrderLineJpaEntity(String sku, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
