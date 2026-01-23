package com.business.banking.services.infrastructure.output.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("movement")
public class MovementEntity {

    @Id
    private UUID id;

    @Column("account_number")
    private String accountNumber;

    @Column("date")
    private LocalDate date;

    @Column("type")
    private String type;

    @Column("value")
    private BigDecimal value;

    @Column("balance")
    private BigDecimal balance;

    @Column("detail")
    private String detail;

    @Column("idempotency_key")
    private String idempotencyKey;
}
