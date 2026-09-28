/*
 ^ TUTORIAL 15 — account lookups, both derived from the method name.
*/
package com.example.bookshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.UserAccount;

/**
 * Database lookups for account authentication and registration.
 *
 * | Key / Method              | Why we use it                                    |
 * |---------------------------|--------------------------------------------------|
 * | JpaRepository             | Supplies standard account CRUD methods          |
 * | findByEmailIgnoreCase     | Finds an account without email case sensitivity |
 * | existsByEmailIgnoreCase   | Prevents duplicate account registrations         |
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	Optional<UserAccount> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
