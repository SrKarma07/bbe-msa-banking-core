package com.business.banking.services.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CustomerFlowIT extends AbstractPostgresIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateCustomer_andReturn201() {
        Map<String, Object> request = Map.of(
                "name", "Jose Lema",
                "gender", "M",
                "age", 30,
                "identification", "EC-0102030405",
                "address", "Otavalo, Ecuador",
                "phoneNumber", "+593-98-254-5785",
                "password", "ChangeMe123!",
                "state", true
        );

        webTestClient.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Jose Lema")
                .jsonPath("$.identification").isEqualTo("EC-0102030405")
                .jsonPath("$.state").isEqualTo(true);
    }

    @Test
    void shouldReturn409_whenIdentificationAlreadyExists() {
        Map<String, Object> request = Map.of(
                "name", "Otra Persona",
                "gender", "F",
                "age", 25,
                "identification", "EC-1102457890",
                "address", "Quito",
                "phoneNumber", "+593-99-000-0000",
                "password", "ChangeMe123!",
                "state", true
        );

        webTestClient.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(409);
    }
}
