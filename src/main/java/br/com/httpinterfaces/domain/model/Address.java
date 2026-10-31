package br.com.httpinterfaces.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 9)
    private String cep;

    @Column(length = 200)
    private String logradouro;

    @Column(length = 200)
    private String complemento;

    @Column(length = 100)
    private String unidade;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String localidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 50)
    private String estado;

    @Column(length = 50)
    private String regiao;

    @Column(length = 20)
    private String ibge;

    @Column(length = 20)
    private String gia;

    @Column(length = 5)
    private String ddd;

    @Column(length = 20)
    private String siafi;

    /** "FEIGN" or "HTTP_INTERFACE" — shows which client fetched this address */
    @Column(name = "fetched_via", nullable = false, length = 20)
    private String fetchedVia;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
