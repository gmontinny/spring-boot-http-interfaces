package br.com.httpinterfaces.application.mapper;

import br.com.httpinterfaces.application.dto.AddressResponse;
import br.com.httpinterfaces.application.dto.ViaCepResponse;
import br.com.httpinterfaces.domain.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(ViaCepResponse dto, String fetchedVia) {
        return Address.builder()
                .cep(dto.getCep())
                .logradouro(dto.getLogradouro())
                .complemento(dto.getComplemento())
                .unidade(dto.getUnidade())
                .bairro(dto.getBairro())
                .localidade(dto.getLocalidade())
                .uf(dto.getUf())
                .estado(dto.getEstado())
                .regiao(dto.getRegiao())
                .ibge(dto.getIbge())
                .gia(dto.getGia())
                .ddd(dto.getDdd())
                .siafi(dto.getSiafi())
                .fetchedVia(fetchedVia)
                .build();
    }

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .cep(address.getCep())
                .logradouro(address.getLogradouro())
                .complemento(address.getComplemento())
                .unidade(address.getUnidade())
                .bairro(address.getBairro())
                .localidade(address.getLocalidade())
                .uf(address.getUf())
                .estado(address.getEstado())
                .regiao(address.getRegiao())
                .ibge(address.getIbge())
                .gia(address.getGia())
                .ddd(address.getDdd())
                .siafi(address.getSiafi())
                .fetchedVia(address.getFetchedVia())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
