package br.com.httpinterfaces.application.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ProductResponse {
    private Long id;
    private String title;
    private String brand;
    private String description;
    private BigDecimal initialPrice;
    private BigDecimal finalPrice;
    private String currency;
    private String availability;
    private BigDecimal rating;
    private Integer reviewsCount;
    private String source;
    private String imageUrl;
    private String productUrl;
    private String sellerName;
    private String categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
