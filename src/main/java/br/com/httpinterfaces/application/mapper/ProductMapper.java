package br.com.httpinterfaces.application.mapper;

import br.com.httpinterfaces.application.dto.ProductRequest;
import br.com.httpinterfaces.application.dto.ProductResponse;
import br.com.httpinterfaces.domain.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest req) {
        return Product.builder()
                .title(req.getTitle())
                .brand(req.getBrand())
                .description(req.getDescription())
                .initialPrice(req.getInitialPrice())
                .finalPrice(req.getFinalPrice())
                .currency(req.getCurrency())
                .availability(req.getAvailability())
                .rating(req.getRating())
                .reviewsCount(req.getReviewsCount() != null ? req.getReviewsCount() : 0)
                .source(req.getSource())
                .imageUrl(req.getImageUrl())
                .productUrl(req.getProductUrl())
                .sellerName(req.getSellerName())
                .categories(req.getCategories())
                .build();
    }

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .brand(p.getBrand())
                .description(p.getDescription())
                .initialPrice(p.getInitialPrice())
                .finalPrice(p.getFinalPrice())
                .currency(p.getCurrency())
                .availability(p.getAvailability())
                .rating(p.getRating())
                .reviewsCount(p.getReviewsCount())
                .source(p.getSource())
                .imageUrl(p.getImageUrl())
                .productUrl(p.getProductUrl())
                .sellerName(p.getSellerName())
                .categories(p.getCategories())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public void updateEntity(Product product, ProductRequest req) {
        product.setTitle(req.getTitle());
        product.setBrand(req.getBrand());
        product.setDescription(req.getDescription());
        product.setInitialPrice(req.getInitialPrice());
        product.setFinalPrice(req.getFinalPrice());
        product.setCurrency(req.getCurrency());
        product.setAvailability(req.getAvailability());
        product.setRating(req.getRating());
        if (req.getReviewsCount() != null) product.setReviewsCount(req.getReviewsCount());
        product.setSource(req.getSource());
        product.setImageUrl(req.getImageUrl());
        product.setProductUrl(req.getProductUrl());
        product.setSellerName(req.getSellerName());
        product.setCategories(req.getCategories());
    }
}
