package br.com.httpinterfaces.domain.repository;

import br.com.httpinterfaces.domain.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCep(String cep);
    List<Address> findByFetchedVia(String fetchedVia);
}
