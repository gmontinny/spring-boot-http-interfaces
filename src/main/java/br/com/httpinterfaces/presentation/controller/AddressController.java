package br.com.httpinterfaces.presentation.controller;

import br.com.httpinterfaces.application.dto.AddressResponse;
import br.com.httpinterfaces.domain.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "ViaCep integration — OpenFeign vs Http Interface")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/feign/{cep}")
    @Operation(summary = "Fetch address via OpenFeign",
               description = "Uses Spring Cloud OpenFeign declarative client to call ViaCep API")
    public ResponseEntity<EntityModel<AddressResponse>> findByFeignCep(@PathVariable String cep) {
        AddressResponse response = addressService.findByCepViaFeign(cep);
        return ResponseEntity.ok(toModel(response));
    }

    @GetMapping("/http-interface/{cep}")
    @Operation(summary = "Fetch address via Http Interface",
               description = "Uses Spring 6 native Http Interface (HttpServiceProxyFactory + RestClient) to call ViaCep API")
    public ResponseEntity<EntityModel<AddressResponse>> findByHttpInterfaceCep(@PathVariable String cep) {
        AddressResponse response = addressService.findByCepViaHttpInterface(cep);
        return ResponseEntity.ok(toModel(response));
    }

    @GetMapping
    @Operation(summary = "List all saved addresses")
    public ResponseEntity<CollectionModel<EntityModel<AddressResponse>>> findAll() {
        List<EntityModel<AddressResponse>> models = addressService.findAll().stream()
                .map(this::toModel).toList();
        CollectionModel<EntityModel<AddressResponse>> collection = CollectionModel.of(models,
                linkTo(methodOn(AddressController.class).findAll()).withSelfRel());
        return ResponseEntity.ok(collection);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find address by ID")
    public ResponseEntity<EntityModel<AddressResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toModel(addressService.findById(id)));
    }

    @GetMapping("/cep/{cep}")
    @Operation(summary = "Find saved addresses by CEP")
    public ResponseEntity<List<AddressResponse>> findByCep(@PathVariable String cep) {
        return ResponseEntity.ok(addressService.findByCep(cep));
    }

    @GetMapping("/source/{fetchedVia}")
    @Operation(summary = "Filter addresses by client used (FEIGN or HTTP_INTERFACE)")
    public ResponseEntity<List<AddressResponse>> findBySource(@PathVariable String fetchedVia) {
        return ResponseEntity.ok(addressService.findByFetchedVia(fetchedVia));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<AddressResponse> toModel(AddressResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(AddressController.class).findById(response.getId())).withSelfRel(),
                linkTo(methodOn(AddressController.class).findAll()).withRel("all-addresses"),
                linkTo(methodOn(AddressController.class).findByCep(response.getCep())).withRel("by-cep"),
                linkTo(methodOn(AddressController.class).findBySource(response.getFetchedVia())).withRel("by-source"));
    }
}
