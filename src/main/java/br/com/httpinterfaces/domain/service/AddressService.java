package br.com.httpinterfaces.domain.service;

import br.com.httpinterfaces.application.dto.AddressResponse;
import br.com.httpinterfaces.application.dto.ViaCepResponse;
import br.com.httpinterfaces.application.mapper.AddressMapper;
import br.com.httpinterfaces.domain.model.Address;
import br.com.httpinterfaces.domain.repository.AddressRepository;
import br.com.httpinterfaces.infrastructure.feign.ViaCepFeignClient;
import br.com.httpinterfaces.infrastructure.httpinterface.ViaCepHttpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final ViaCepFeignClient feignClient;
    private final ViaCepHttpInterface httpInterface;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    /**
     * Busca CEP via OpenFeign e persiste o resultado.
     * OpenFeign: client declarativo do Spring Cloud, proxy gerado automaticamente.
     */
    @Transactional
    public AddressResponse findByCepViaFeign(String cep) {
        ViaCepResponse response = feignClient.findByCep(sanitizeCep(cep));
        validateResponse(response, cep);
        Address saved = addressRepository.save(addressMapper.toEntity(response, "FEIGN"));
        return addressMapper.toResponse(saved);
    }

    /**
     * Busca CEP via Http Interface e persiste o resultado.
     * Http Interface: client declarativo nativo Spring 6+, proxy criado via HttpServiceProxyFactory.
     */
    @Transactional
    public AddressResponse findByCepViaHttpInterface(String cep) {
        ViaCepResponse response = httpInterface.findByCep(sanitizeCep(cep));
        validateResponse(response, cep);
        Address saved = addressRepository.save(addressMapper.toEntity(response, "HTTP_INTERFACE"));
        return addressMapper.toResponse(saved);
    }

    public List<AddressResponse> findAll() {
        return addressRepository.findAll().stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    public AddressResponse findById(Long id) {
        return addressRepository.findById(id)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
    }

    public List<AddressResponse> findByCep(String cep) {
        return addressRepository.findByCep(cep).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    public List<AddressResponse> findByFetchedVia(String fetchedVia) {
        return addressRepository.findByFetchedVia(fetchedVia.toUpperCase()).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!addressRepository.existsById(id))
            throw new IllegalArgumentException("Address not found: " + id);
        addressRepository.deleteById(id);
    }

    private String sanitizeCep(String cep) {
        return cep.replaceAll("[^0-9]", "");
    }

    private void validateResponse(ViaCepResponse response, String cep) {
        if (response == null || Boolean.TRUE.equals(response.getErro()))
            throw new IllegalArgumentException("CEP not found: " + cep);
    }
}
