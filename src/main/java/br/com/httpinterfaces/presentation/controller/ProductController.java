package br.com.httpinterfaces.presentation.controller;

import br.com.httpinterfaces.application.dto.ProductRequest;
import br.com.httpinterfaces.application.dto.ProductResponse;
import br.com.httpinterfaces.domain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD — data imported from Amazon, Shopee, Lazada, Shein, Walmart CSVs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List all products (paginated)")
    public ResponseEntity<Page<EntityModel<ProductResponse>>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable).map(this::toModel));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find product by ID")
    public ResponseEntity<EntityModel<ProductResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toModel(productService.findById(id)));
    }

    @GetMapping("/source/{source}")
    @Operation(summary = "Filter products by source (amazon, shopee, lazada, shein, walmart)")
    public ResponseEntity<Page<EntityModel<ProductResponse>>> findBySource(
            @PathVariable String source,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.findBySource(source, pageable).map(this::toModel));
    }

    @GetMapping("/brand/{brand}")
    @Operation(summary = "Filter products by brand")
    public ResponseEntity<Page<EntityModel<ProductResponse>>> findByBrand(
            @PathVariable String brand,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.findByBrand(brand, pageable).map(this::toModel));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by title keyword")
    public ResponseEntity<Page<EntityModel<ProductResponse>>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.search(keyword, pageable).map(this::toModel));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EntityModel<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = linkTo(methodOn(ProductController.class).findById(created.getId())).toUri();
        return ResponseEntity.created(location).body(toModel(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EntityModel<ProductResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(toModel(productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<ProductResponse> toModel(ProductResponse product) {
        return EntityModel.of(product,
                linkTo(methodOn(ProductController.class).findById(product.getId())).withSelfRel(),
                linkTo(methodOn(ProductController.class).findAll(Pageable.unpaged())).withRel("all-products"),
                linkTo(methodOn(ProductController.class).findBySource(product.getSource(), Pageable.unpaged())).withRel("by-source"),
                linkTo(methodOn(ProductController.class).findByBrand(product.getBrand(), Pageable.unpaged())).withRel("by-brand"));
    }
}
