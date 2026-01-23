package com.business.banking.services.infrastructure.output.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Table("account")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountEntity implements Persistable<String> {

    @Id
    @Column("number")
    private String number;

    @Column("type")
    private String type;

    @Column("balance")
    private BigDecimal balance;

    @Column("state")
    private Boolean state;

    @Column("customer_id")
    private UUID customerId;

    @Transient
    private boolean isNew;

    @Override
    public String getId() {
        return this.number;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
