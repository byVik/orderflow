package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.web.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Catalog")
@RequiredArgsConstructor
class ProductController {

    private final ListProductsQuery products;

    @GetMapping
    @Operation(summary = "Lists the catalog, or only the SKUs given in ?skus=A,B")
    List<ProductResponse> list(@RequestParam(required = false) List<String> skus) {
        List<Product> result = (skus == null || skus.isEmpty()) ? products.listAll() : products.findBySkus(skus);
        return result.stream().map(ProductResponse::from).toList();
    }
}
