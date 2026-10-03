package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.inventory.application.port.in.ListProductsQuery;
import dev.viktorstrohush.orderflow.inventory.domain.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Catálogo")
class ProductController {

    private final ListProductsQuery products;

    ProductController(ListProductsQuery products) {
        this.products = products;
    }

    @GetMapping
    @Operation(summary = "Lista el catálogo, o solo los SKUs indicados en ?skus=A,B")
    List<ProductResponse> list(@RequestParam(required = false) List<String> skus) {
        List<Product> result = (skus == null || skus.isEmpty()) ? products.listAll() : products.findBySkus(skus);
        return result.stream().map(ProductResponse::from).toList();
    }

    record ProductResponse(String sku, String name, BigDecimal price, int availableQuantity) {
        static ProductResponse from(Product p) {
            return new ProductResponse(p.sku(), p.name(), p.price(), p.availableQuantity());
        }
    }
}
