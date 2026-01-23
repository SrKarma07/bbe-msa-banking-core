package com.business.banking.services.infrastructure.output.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("person_id")
    private Long personId;

    @Column("password")
    private String password;

    @Column("state")
    private Boolean state;

    @Transient
    private boolean isNew;

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
