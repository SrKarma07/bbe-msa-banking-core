package com.business.banking.services.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class MovementFlowIT extends AbstractPostgresIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateDepositMovement_andUpdateAccountBalance() {
        String accountNumber = "ACC-100001";
        BigDecimal deposit = new BigDecimal("100.00");
        LocalDate date = LocalDate.parse("2026-01-22");

        Map<String, Object> request = Map.of(
                "accountNumber", accountNumber,
                "date", date.toString(),
                "type", "Deposito",
                "value", deposit,
                "detail", "IT deposit"
        );

        webTestClient.post()
                .uri("/api/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "it-acc-100001-dep-001")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(accountNumber)
                .jsonPath("$.type").isEqualTo("Deposito")
                .jsonPath("$.value").isEqualTo(100.00)
                .jsonPath("$.balance").isEqualTo(1600.00);

        webTestClient.get()
                .uri("/api/accounts/{number}", accountNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(accountNumber)
                .jsonPath("$.balance").isEqualTo(1600.00);
    }
}
