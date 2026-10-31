package br.com.httpinterfaces.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** ViaCep API response payload */
@Data
public class ViaCepResponse {
    private String cep;
    private String logradouro;
    private String complemento;
    private String unidade;
    private String bairro;
    private String localidade;
    private String uf;
    private String estado;
    private String regiao;
    private String ibge;
    private String gia;
    private String ddd;
    private String siafi;

    @JsonProperty("erro")
    private Boolean erro;
}
