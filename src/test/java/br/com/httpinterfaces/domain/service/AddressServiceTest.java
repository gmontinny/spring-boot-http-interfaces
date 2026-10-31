package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.AddressResponse;
import br.com.httpinterfaces.application.dto.ViaCepResponse;
import br.com.httpinterfaces.application.mapper.AddressMapper;
import br.com.httpinterfaces.domain.model.Address;
import br.com.httpinterfaces.domain.repository.AddressRepository;
import br.com.httpinterfaces.infrastructure.feign.ViaCepFeignClient;
import br.com.httpinterfaces.infrastructure.httpinterface.ViaCepHttpInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock ViaCepFeignClient feignClient;
    @Mock ViaCepHttpInterface httpInterface;
    @Mock AddressRepository addressRepository;
    @Mock AddressMapper addressMapper;

    @InjectMocks
    AddressService addressService;

    private ViaCepResponse validResponse() {
        ViaCepResponse r = new ViaCepResponse();
        r.setCep("01001-000");
        r.setLogradouro("Praça da Sé");
        r.setLocalidade("São Paulo");
        r.setUf("SP");
        return r;
    }

    private Address savedAddress(String fetchedVia) {
        return Address.builder()
                .id(1L)
                .cep("01001000")
                .logradouro("Praça da Sé")
                .localidade("São Paulo")
                .uf("SP")
                .fetchedVia(fetchedVia)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AddressResponse addressResponse(String fetchedVia) {
        return AddressResponse.builder()
                .id(1L).cep("01001000").fetchedVia(fetchedVia).build();
    }

    @Test
    void findByCepViaFeign_shouldPersistWithFeignSource() {
        ViaCepResponse apiResponse = validResponse();
        Address entity = savedAddress("FEIGN");
        AddressResponse expected = addressResponse("FEIGN");

        when(feignClient.findByCep("01001000")).thenReturn(apiResponse);
        when(addressMapper.toEntity(apiResponse, "FEIGN")).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressMapper.toResponse(entity)).thenReturn(expected);

        AddressResponse result = addressService.findByCepViaFeign("01001-000");

        assertThat(result.getFetchedVia()).isEqualTo("FEIGN");
        verify(feignClient).findByCep("01001000");
        verify(addressRepository).save(entity);
    }

    @Test
    void findByCepViaHttpInterface_shouldPersistWithHttpInterfaceSource() {
        ViaCepResponse apiResponse = validResponse();
        Address entity = savedAddress("HTTP_INTERFACE");
        AddressResponse expected = addressResponse("HTTP_INTERFACE");

        when(httpInterface.findByCep("01001000")).thenReturn(apiResponse);
        when(addressMapper.toEntity(apiResponse, "HTTP_INTERFACE")).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressMapper.toResponse(entity)).thenReturn(expected);

        AddressResponse result = addressService.findByCepViaHttpInterface("01001000");

        assertThat(result.getFetchedVia()).isEqualTo("HTTP_INTERFACE");
        verify(httpInterface).findByCep("01001000");
    }

    @Test
    void findByCepViaFeign_shouldThrowWhenCepNotFound() {
        ViaCepResponse notFound = new ViaCepResponse();
        notFound.setErro(true);
        when(feignClient.findByCep("00000000")).thenReturn(notFound);

        assertThatThrownBy(() -> addressService.findByCepViaFeign("00000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CEP not found");
    }

    @Test
    void findAll_shouldReturnMappedList() {
        Address entity = savedAddress("FEIGN");
        AddressResponse response = addressResponse("FEIGN");

        when(addressRepository.findAll()).thenReturn(List.of(entity));
        when(addressMapper.toResponse(entity)).thenReturn(response);

        List<AddressResponse> result = addressService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFetchedVia()).isEqualTo("FEIGN");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(addressRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> addressService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void findByFetchedVia_shouldNormalizeToUpperCase() {
        Address entity = savedAddress("FEIGN");
        AddressResponse response = addressResponse("FEIGN");

        when(addressRepository.findByFetchedVia("FEIGN")).thenReturn(List.of(entity));
        when(addressMapper.toResponse(entity)).thenReturn(response);

        List<AddressResponse> result = addressService.findByFetchedVia("feign");

        assertThat(result).hasSize(1);
        verify(addressRepository).findByFetchedVia("FEIGN");
    }
}
