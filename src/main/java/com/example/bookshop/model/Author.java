/*
 ^ TUTORIAL 10 — the "one" side of one-to-many

 ? One author has many books; each book has one author. In the
 ? database that is a single foreign-key column: book.author_id.

 ? This side does NOT own the relationship:
 ?   mappedBy = "author" says "the mapping lives on Book.author -
 ?   I am just the mirror". Only the owning side (the one with the
 ?   foreign key) writes to the database.
 ! Forget mappedBy and JPA invents a THIRD table (author_books) to
 !   track the same fact twice. If a mystery join table appears in
 !   your schema, look for a missing mappedBy.

 ? fetch = LAZY (the default for collections): loading an Author
 ? does NOT load their books - a proxy list sits there until someone
 ? calls books.get(0) or iterates. That deferred loading is exactly
 ? what causes the N+1 problem demonstrated in this tutorial, and
 ? the LazyInitializationException in tutorial 12.
*/
package com.example.bookshop.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Author {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@OneToMany(mappedBy = "author")
	private List<Book> books = new ArrayList<>();

	protected Author() {
		// for JPA only
	}

	public Author(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Book> getBooks() {
		return books;
	}
}
