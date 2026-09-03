/*
 ^ TUTORIAL 10 — one side of many-to-many

 ? A book can have several genres; a genre contains many books.
 ? No foreign key column can express that - it needs a JOIN TABLE
 ? (book_genre) with one row per (book, genre) pair. Book owns the
 ? relationship and defines that table; see Book.java.

 * This entity does not even map the reverse collection: nothing in
 * the app needs genre.getBooks(), and every mapped collection is
 * one more thing to fetch wrong. Map what you use.
*/
package com.example.bookshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Genre {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	protected Genre() {
		// for JPA only
	}

	public Genre(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
