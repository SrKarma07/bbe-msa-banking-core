package com.business.banking.services.application.service;

import com.business.banking.services.application.input.port.CustomerServicePort;
import com.business.banking.services.application.output.port.CustomerRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.DuplicateIdentificationException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.domain.model.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Service
@RequiredArgsConstructor
public class CustomerService implements CustomerServicePort {

    private final CustomerRepositoryPort repo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Flux<Customer> list() {
        return repo.findAll();
    }

    @Override
    public Mono<PageResult<Customer>> listPage(Boolean state, String identification, String name, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        int offset = safePage * safeSize;
        int limit = safeSize;

        return repo.count(state, identification, name)
                .flatMap(total -> {
                    int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
                    var items = repo.findPage(state, identification, name, offset, limit);
                    return Mono.just(new PageResult<>(items, safePage, safeSize, total, totalPages));
                });
    }

    @Override
    public Mono<Customer> getById(UUID id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(NotFoundException.customerById(id.toString())));
    }

    @Override
    public Mono<Customer> create(Customer c) {
        if (c == null) return Mono.error(new InvalidRequestException("Customer body cannot be null"));
        if (c.getPerson() == null) return Mono.error(InvalidRequestException.fieldMissing("person"));

        validatePersonForCreate(c.getPerson());

        if (c.getPassword() == null || c.getPassword().isBlank()) {
            return Mono.error(InvalidRequestException.fieldMissing("password"));
        }
        if (c.getPassword().length() < 8) {
            return Mono.error(new InvalidRequestException("password must be at least 8 characters"));
        }
        if (c.getState() == null) {
            return Mono.error(InvalidRequestException.fieldMissing("state"));
        }

        UUID id = (c.getId() != null) ? c.getId() : UUID.randomUUID();

        Customer toCreate = Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name(c.getPerson().getName())
                        .gender(c.getPerson().getGender())
                        .age(c.getPerson().getAge())
                        .identification(c.getPerson().getIdentification())
                        .address(c.getPerson().getAddress())
                        .phoneNumber(c.getPerson().getPhoneNumber())
                        .build())
                .password(passwordEncoder.encode(c.getPassword()))
                .state(c.getState())
                .build();

        return repo.existsByIdentification(toCreate.getPerson().getIdentification())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateIdentificationException(toCreate.getPerson().getIdentification()))
                        : repo.create(toCreate));
    }

    @Override
    public Mono<Customer> update(UUID id, Customer patch) {
        return getById(id).flatMap(actual -> {
            Customer p = (patch != null) ? patch : Customer.builder().build();
            Person personPatch = p.getPerson();

            String newIdentification = (personPatch != null) ? personPatch.getIdentification() : null;
            boolean identificationChanged = newIdentification != null
                    && !newIdentification.isBlank()
                    && !newIdentification.equals(actual.getPerson().getIdentification());

            Mono<Void> identificationGuard = identificationChanged
                    ? repo.existsByIdentificationAndDifferentCustomer(id, newIdentification)
                    .flatMap(exists -> exists
                            ? Mono.error(new DuplicateIdentificationException(newIdentification))
                            : Mono.empty())
                    : Mono.empty();

            return identificationGuard.then(Mono.defer(() -> {
                Person mergedPerson = Person.builder()
                        .name(firstNonNull(personPatch != null ? personPatch.getName() : null, actual.getPerson().getName()))
                        .gender(firstNonNull(personPatch != null ? personPatch.getGender() : null, actual.getPerson().getGender()))
                        .age(firstNonNull(personPatch != null ? personPatch.getAge() : null, actual.getPerson().getAge()))
                        .identification(firstNonNull(personPatch != null ? personPatch.getIdentification() : null, actual.getPerson().getIdentification()))
                        .address(firstNonNull(personPatch != null ? personPatch.getAddress() : null, actual.getPerson().getAddress()))
                        .phoneNumber(firstNonNull(personPatch != null ? personPatch.getPhoneNumber() : null, actual.getPerson().getPhoneNumber()))
                        .build();

                String mergedPassword = actual.getPassword();
                if (p.getPassword() != null) {
                    if (p.getPassword().isBlank()) {
                        throw new InvalidRequestException("password cannot be blank");
                    }
                    if (p.getPassword().length() < 8) {
                        throw new InvalidRequestException("password must be at least 8 characters");
                    }
                    mergedPassword = passwordEncoder.encode(p.getPassword());
                }

                Customer merged = Customer.builder()
                        .id(actual.getId())
                        .person(mergedPerson)
                        .password(mergedPassword)
                        .state(firstNonNull(p.getState(), actual.getState()))
                        .build();

                // IMPORTANT: repo.update updates same person_id
                return repo.update(id, merged);
            }));
        });
    }

    @Override
    public Mono<Void> delete(UUID id) {
        return getById(id).flatMap(c -> repo.deleteById(c.getId()));
    }

    private static void validatePersonForCreate(Person person) {
        if (person.getName() == null || person.getName().isBlank()) {
            throw InvalidRequestException.fieldMissing("person.name");
        }
        if (person.getGender() == null || person.getGender().isBlank()) {
            throw InvalidRequestException.fieldMissing("person.gender");
        }
        if (!person.getGender().matches("^(M|F|X)$")) {
            throw new InvalidRequestException("person.gender must match pattern ^(M|F|X)$");
        }
        if (person.getAge() == null) {
            throw InvalidRequestException.fieldMissing("person.age");
        }
        if (person.getAge() < 0 || person.getAge() > 125) {
            throw new InvalidRequestException("person.age must be between 0 and 125");
        }
        if (person.getIdentification() == null || person.getIdentification().isBlank()) {
            throw InvalidRequestException.fieldMissing("person.identification");
        }
        if (person.getIdentification().length() < 3 || person.getIdentification().length() > 50) {
            throw new InvalidRequestException("person.identification length must be between 3 and 50");
        }
        if (person.getAddress() == null || person.getAddress().isBlank()) {
            throw InvalidRequestException.fieldMissing("person.address");
        }
        if (person.getPhoneNumber() == null || person.getPhoneNumber().isBlank()) {
            throw InvalidRequestException.fieldMissing("person.phoneNumber");
        }
    }
}
