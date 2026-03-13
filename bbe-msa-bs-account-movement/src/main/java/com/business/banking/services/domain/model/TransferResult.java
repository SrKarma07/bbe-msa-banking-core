package com.business.banking.services.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TransferResult {
    private Movement debitMovement;
    private Movement creditMovement;
}
