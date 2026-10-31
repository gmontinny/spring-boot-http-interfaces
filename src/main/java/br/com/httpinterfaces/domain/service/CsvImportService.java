package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.domain.model.Product;
import br.com.httpinterfaces.domain.repository.ProductRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final ProductRepository productRepository;

    @Transactional
    public int importFromFile(MultipartFile file, String source) throws IOException, CsvException {
        List<Product> products = new ArrayList<>();

        var csvParser = new CSVParserBuilder()
                .withIgnoreQuotations(false)
                .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(csvParser)
                .withKeepCarriageReturn(false)
                .withVerifyReader(false)
                .withSkipLines(0)
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return 0;

            String[] row;
            int lineNum = 1;
            while ((row = safeReadNext(reader, lineNum)) != null) {
                lineNum++;
                try {
                    Product product = parseRow(headers, row, source);
                    if (product != null) products.add(product);
                } catch (Exception e) {
                    log.warn("Skipping row {} due to parse error: {}", lineNum, e.getMessage());
                }
            }
        }

        productRepository.saveAll(products);
        log.info("Imported {} products from source: {}", products.size(), source);
        return products.size();
    }

    private Product parseRow(String[] headers, String[] row, String source) {
        if (row.length < 2) return null;

        // title: amazon/shopee/lazada use "title", shein/walmart use "product_name"
        String title = firstNonNull(headers, row, "title", "product_name");
        if (title == null || title.isBlank()) return null;

        // reviews: amazon/shein use "reviews_count", shopee/lazada use "reviews", walmart uses "review_count"
        String reviews = firstNonNull(headers, row, "reviews_count", "reviews", "review_count");

        // image: amazon uses "image_url", lazada/shopee use "image", walmart uses "main_image"
        String image = firstNonNull(headers, row, "image_url", "image", "main_image");

        // description: amazon/lazada use "description", shein uses "description", walmart uses "description"
        // lazada uses "product_description", shein uses "description"
        String description = firstNonNull(headers, row, "description", "product_description", "Product Description");

        // categories: amazon uses "categories", shein uses "category_tree", lazada/shopee use "breadcrumb"
        String categories = firstNonNull(headers, row, "categories", "category_tree", "breadcrumb");

        // availability: amazon uses "availability", shopee uses "stock", walmart uses "available_for_delivery"
        String availability = firstNonNull(headers, row, "availability", "stock", "in_stock");

        // external id: amazon uses "asin", shopee uses "id", lazada/walmart/shein use "sku"/"product_id"
        String externalId = firstNonNull(headers, row, "asin", "id", "sku", "product_id");

        return Product.builder()
                .title(truncate(title, 500))
                .brand(truncate(getColumn(headers, row, "brand"), 200))
                .description(description)
                .initialPrice(parsePrice(getColumn(headers, row, "initial_price")))
                .finalPrice(parsePrice(getColumn(headers, row, "final_price")))
                .currency(truncate(getColumn(headers, row, "currency"), 10))
                .availability(truncate(availability, 100))
                .rating(parseRating(getColumn(headers, row, "rating")))
                .reviewsCount(parseInt(reviews))
                .source(source)
                .externalId(truncate(externalId, 200))
                .imageUrl(image)
                .productUrl(getColumn(headers, row, "url"))
                .sellerName(truncate(getColumn(headers, row, "seller_name"), 200))
                .categories(categories)
                .build();
    }

    private String firstNonNull(String[] headers, String[] row, String... names) {
        for (String name : names) {
            String val = getColumn(headers, row, name);
            if (val != null) return val;
        }
        return null;
    }

    private String getColumn(String[] headers, String[] row, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(name) && i < row.length) {
                String raw = row[i];
                if (raw == null) return null;
                String val = raw.trim().replaceAll("^\"|\"$", "");
                return val.isBlank() || val.equalsIgnoreCase("null") ? null : val;
            }
        }
        return null;
    }

    private String[] safeReadNext(CSVReader reader, int lineNum) {
        try {
            return reader.readNext();
        } catch (Exception e) {
            log.warn("Skipping malformed line {}: {}", lineNum, e.getMessage());
            return new String[0]; // triggers null-check in parseRow
        }
    }

    private BigDecimal parsePrice(String value) {
        BigDecimal price = parseBigDecimal(value);
        if (price == null) return null;
        // NUMERIC(15,2): max 13 digits before decimal — discard absurd values
        if (price.compareTo(new BigDecimal("9999999999999")) > 0) return null;
        if (price.compareTo(BigDecimal.ZERO) < 0) return null;
        return price.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return null;
        try {
            // handles scientific notation (e.g. "1.58095e+03" from Shopee)
            return new BigDecimal(value.trim()).stripTrailingZeros();
        } catch (Exception e) {
            try {
                // fallback: strip non-numeric chars for values like "$12.99"
                String cleaned = value.replaceAll("[^0-9.]", "");
                return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private BigDecimal parseRating(String value) {
        BigDecimal rating = parseBigDecimal(value);
        if (rating == null) return null;
        // clamp to valid range [0, 9.9] — NUMERIC(4,2) max meaningful rating
        if (rating.compareTo(BigDecimal.TEN) >= 0) return null;
        if (rating.compareTo(BigDecimal.ZERO) < 0) return null;
        return rating;
    }

    private Integer parseInt(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
