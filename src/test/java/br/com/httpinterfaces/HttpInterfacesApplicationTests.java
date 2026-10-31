package br.com.httpinterfaces;

import br.com.httpinterfaces.infrastructure.feign.ViaCepFeignClient;
import br.com.httpinterfaces.infrastructure.httpinterface.ViaCepHttpInterface;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class HttpInterfacesApplicationTests {

    @MockitoBean
    ViaCepFeignClient viaCepFeignClient;

    @MockitoBean
    ViaCepHttpInterface viaCepHttpInterface;

    @Test
    void contextLoads() {
    }
}
