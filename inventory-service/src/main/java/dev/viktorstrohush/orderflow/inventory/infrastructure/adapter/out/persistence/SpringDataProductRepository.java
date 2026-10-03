package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, String> {

    List<ProductJpaEntity> findAllByOrderByNameAsc();

    List<ProductJpaEntity> findBySkuIn(Collection<String> skus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductJpaEntity p where p.sku in :skus order by p.sku")
    List<ProductJpaEntity> findBySkuInForUpdate(Collection<String> skus);
}
