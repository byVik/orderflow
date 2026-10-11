package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    /**
     * Slice and not Page: it asks for one row more than the page to know whether there is a next
     * one, instead of running a count over all the customer's orders.
     *
     * <p>No @EntityGraph on the lines: a join on a collection multiplies the rows, so Hibernate
     * could not apply LIMIT in SQL and would paginate in memory. The lines come in a second query
     * (see @BatchSize in {@link OrderJpaEntity}).
     */
    Slice<OrderJpaEntity> findByCustomerId(String customerId, Pageable pageable);
}
