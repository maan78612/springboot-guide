/*
 ^ TUTORIAL 15 — the account entity

 ? Named UserAccount, not User: USER is a reserved word in several
 ? databases (Postgres included - tutorial 18) and fighting the
 ? reserved word is not worth three saved characters.

 ? passwordHash stores the BCRYPT HASH of the password - never the
 ? password. A hash is one-way: you can check a guess against it,
 ? but not compute the password back. BCrypt also salts (two users
 ? with the same password get different hashes) and is deliberately
 ? slow, which is what you want against brute force.
 ! If you can SELECT a column and read anyone's password, you built
 !   it wrong. There is no "decrypt" step anywhere in this app.

 ? @Enumerated(EnumType.STRING) stores the role as text ("ADMIN").
 ! The default, ORDINAL, stores the enum's position (0, 1) - and
 !   reordering the enum later silently changes every user's role.
 !   Always STRING.
*/
package com.example.bookshop.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * JPA entity for an account; passwords are stored only as hashes.
 *
 * | Key                           | Why we use it                                       |
 * |-------------------------------|-----------------------------------------------------|
 * | @Entity                       | Maps account objects to user_account rows           |
 * | @Id / @GeneratedValue         | Marks the database-generated primary key             |
 * | @Column(unique, nullable=false)| Requires each account to have a unique email       |
 * | passwordHash                  | Stores a one-way password hash, never the raw value |
 * | @Enumerated(EnumType.STRING)  | Stores role names, avoiding fragile numeric ordinals|
 * | Instant                       | Records creation time as a UTC timeline value       |
 */
@Entity
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private Instant createdAt;

	protected UserAccount() {
		// for JPA only
	}

	public UserAccount(String name, String email, String passwordHash, Role role) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
