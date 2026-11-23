package com.delta.bank.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repozytorium do odczytu i zapisu encji rachunków bankowych.
 * Numer rachunku jest kluczem głównym.
 */
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, String> {

    // Ta kwerenda pobiera rachunki o podanych numerach i — dzięki `@Lock(PESSIMISTIC_WRITE)`
    // — blokuje je na czas transakcji, żeby równoległa operacja nie zmieniła ich w trakcie, np. przelewu.
    //`order by a.number` zwraca je w stałej kolejności. To ma ograniczać ryzyko zakleszczeń, gdy różne transakcje blokują te same rachunki.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select a
    from BankAccountEntity a
    where a.number in :numbers
    order by a.number
    """)
    List<BankAccountEntity> findAllForUpdateOrderByNumber(@Param("numbers") Collection<String> numbers);
}