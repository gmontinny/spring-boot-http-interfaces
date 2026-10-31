package br.com.httpinterfaces.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 200)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "initial_price", precision = 15, scale = 2)
    private BigDecimal initialPrice;

    @Column(name = "final_price", precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @Column(length = 10)
    private String currency;

    @Column(length = 100)
    private String availability;

    @Column(precision = 4, scale = 2)
    private BigDecimal rating;

    @Builder.Default
    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(columnDefinition = "TEXT")
    private String categories;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
