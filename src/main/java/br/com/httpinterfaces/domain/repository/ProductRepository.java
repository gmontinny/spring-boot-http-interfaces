package br.com.httpinterfaces.domain.repository;

import br.com.httpinterfaces.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findBySource(String source, Pageable pageable);

    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
