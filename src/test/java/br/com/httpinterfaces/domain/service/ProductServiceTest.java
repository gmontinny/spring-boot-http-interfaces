package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.ProductRequest;
import br.com.httpinterfaces.application.dto.ProductResponse;
import br.com.httpinterfaces.application.mapper.ProductMapper;
import br.com.httpinterfaces.domain.model.Product;
import br.com.httpinterfaces.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;

    @InjectMocks
    ProductService productService;

    private Product product() {
        return Product.builder().id(1L).title("Test Product").source("amazon")
                .brand("BrandX").finalPrice(BigDecimal.TEN).build();
    }

    private ProductResponse productResponse() {
        return ProductResponse.builder().id(1L).title("Test Product")
                .source("amazon").brand("BrandX").finalPrice(BigDecimal.TEN).build();
    }

    private ProductRequest productRequest() {
        ProductRequest req = new ProductRequest();
        req.setTitle("Test Product");
        req.setSource("amazon");
        req.setBrand("BrandX");
        req.setFinalPrice(BigDecimal.TEN);
        return req;
    }

    @Test
    void findAll_shouldReturnPagedProducts() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(product()));

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(productResponse());

        Page<ProductResponse> result = productService.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));
        when(productMapper.toResponse(any())).thenReturn(productResponse());

        ProductResponse result = productService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Product");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void create_shouldSaveAndReturnProduct() {
        ProductRequest request = productRequest();
        Product entity = product();

        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(productResponse());

        ProductResponse result = productService.create(request);

        assertThat(result.getTitle()).isEqualTo("Test Product");
        verify(productRepository).save(entity);
    }

    @Test
    void update_shouldModifyAndReturnProduct() {
        ProductRequest request = productRequest();
        request.setTitle("Updated");
        Product entity = product();

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(productResponse());

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isNotNull();
        verify(productMapper).updateEntity(entity, request);
        verify(productRepository).save(entity);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, productRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void delete_shouldCallRepository() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void findBySource_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(product()));

        when(productRepository.findBySource("amazon", pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(productResponse());

        Page<ProductResponse> result = productService.findBySource("amazon", pageable);

        assertThat(result.getContent().get(0).getSource()).isEqualTo("amazon");
    }

    @Test
    void search_shouldReturnMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(product()));

        when(productRepository.searchByTitle("Test", pageable)).thenReturn(page);
        when(productMapper.toResponse(any())).thenReturn(productResponse());

        Page<ProductResponse> result = productService.search("Test", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
