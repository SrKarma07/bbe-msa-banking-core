package com.business.banking.services.infrastructure.output.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonEntity implements Persistable<Long> {

    @Id
    @Column("person_id")
    private Long personId;

    @Column("name")
    private String name;

    @Column("gender")
    private String gender;

    @Column("age")
    private Integer age;

    @Column("identification")
    private String identification;

    @Column("address")
    private String address;

    @Column("phone_number")
    private String phoneNumber;

    @Transient
    private boolean isNew;

    @Override
    public Long getId() {
        return this.personId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
