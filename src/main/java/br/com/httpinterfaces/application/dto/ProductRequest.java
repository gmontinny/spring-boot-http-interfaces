package br.com.httpinterfaces.application.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 200)
    private String brand;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Initial price must be positive")
    private BigDecimal initialPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Final price must be positive")
    private BigDecimal finalPrice;

    @Size(max = 10)
    private String currency;

    @Size(max = 100)
    private String availability;

    @DecimalMin("0.0") @DecimalMax("5.0")
    private BigDecimal rating;

    @Min(0)
    private Integer reviewsCount;

    @NotBlank(message = "Source is required")
    @Size(max = 50)
    private String source;

    private String imageUrl;
    private String productUrl;
    private String sellerName;
    private String categories;
}
