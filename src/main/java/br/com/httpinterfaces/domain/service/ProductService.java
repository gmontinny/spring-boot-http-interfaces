package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.ProductRequest;
import br.com.httpinterfaces.application.dto.ProductResponse;
import br.com.httpinterfaces.application.mapper.ProductMapper;
import br.com.httpinterfaces.domain.model.Product;
import br.com.httpinterfaces.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    public Page<ProductResponse> findBySource(String source, Pageable pageable) {
        return productRepository.findBySource(source, pageable).map(productMapper::toResponse);
    }

    public Page<ProductResponse> findByBrand(String brand, Pageable pageable) {
        return productRepository.findByBrandIgnoreCase(brand, pageable).map(productMapper::toResponse);
    }

    public Page<ProductResponse> search(String keyword, Pageable pageable) {
        return productRepository.searchByTitle(keyword, pageable).map(productMapper::toResponse);
    }

    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product saved = productRepository.save(productMapper.toEntity(request));
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        productMapper.updateEntity(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id))
            throw new IllegalArgumentException("Product not found: " + id);
        productRepository.deleteById(id);
    }
}
