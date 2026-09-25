/*
 ^ TUTORIAL 02 — A plain object that will travel as JSON
 ? Jackson is a library that converts Java objects to JSON (and
 ?   JSON back to Java objects). Spring Boot includes it for you.
 ?   When a controller returns a Book, Jackson turns it into JSON
 ?   before it is sent to the client.
 ?
 ? In Java, fields are usually private and read through public
 ?   getters. This is the normal Java way.
 ?
 ? Jackson turns this object into JSON using those getters.
 ?   Example:
 ?     private String title = "Effective Java";
 ?     public String getTitle() { return title; }
 ?   JSON:
 ?     {"title": "Effective Java"}
 ?
 ? The JSON name comes from the getter's name, not the field's.
 ?   Example:
 ?     public String getTitleeeee() { return title; }
 ?   JSON:
 ?     {"titleeeee": "Effective Java"}
 ?
 ! If a getter is not public, Jackson can't see it, and that
 !   field is missing from the JSON. There is no error.
 !   Example:
 !     String getTitle() { return title; }   // no "public"
 !   JSON:
 !     {}

 ^ TUTORIAL 05 — the same class, now a database row
 ? @Entity: one object = one row. @Id: primary key.
 ? @GeneratedValue(IDENTITY): the database assigns ids.
 ! No no-arg constructor -> app starts fine, first query dies with
 !   HTTP 500 "No default constructor for entity". Check it first.

 ^ TUTORIAL 07 — costPrice is internal; DTOs keep it off the API.

 ^ TUTORIAL 10 — relationships

 ? author (was a String) is now a real reference:
 ?   @ManyToOne - many books point at one author.
 ?   This is the OWNING side: it maps the book.author_id foreign-key
 ?   column. The Author.books list is just the mirror (mappedBy).
 ?   optional = false -> a book without an author cannot be saved.
 + fetch = LAZY on purpose: JPA defaults @ManyToOne to EAGER (load
 +   the author every time a book loads, needed or not). LAZY loads
 +   it on first use. Rule: make everything LAZY, fetch eagerly per
 +   query where needed (join fetch - see BookRepository).

 ? genres is @ManyToMany: needs a join table, book_genre, declared
 ?   here on the owning side - two columns, one row per pair.
 ? Set, not List: a book has a genre once; no meaningful order.

 ^ TUTORIAL 11 — @SoftDelete: rows are marked, never removed

 ? One annotation changes three behaviors:
 ?   1. schema gains a "deleted boolean not null" column
 ?   2. repository.delete(book) runs UPDATE ... set deleted=true
 ?   3. EVERY Hibernate query silently appends "and deleted=false" -
 ?      findAll, findById, derived queries, our @Query search, all of
 ?      them. Deleted rows become invisible.
 ? (The node reference needed a hand-written Mongoose plugin for
 ? this; here it is built into Hibernate.)
 ! NATIVE queries (nativeQuery = true) are NOT rewritten - they see
 !   deleted rows. That is a trap and a tool: BookRepository uses it
 !   deliberately for findDeleted() and restore.

 ^ TUTORIAL 16 — ownership

 ? owner = the seller account that listed this book. NULL for the
 ? seeded "house stock" books (only admins may touch those).
 ? Sellers modify their own books; admins modify anything - the
 ? check lives in BookService.assertCanModify.
*/
package com.example.bookshop.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SoftDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
@SoftDelete(columnName = "deleted")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id")
	private Author author;

	// @BatchSize: when one book's lazy genres load, Hibernate loads
	// them for up to 50 books of this page in the same IN-query -
	// 10 books = 1 genre query instead of 10. (Collection fetch joins
	// don't mix with pagination; this is the pragmatic fix.)
	@ManyToMany(fetch = FetchType.LAZY)
	@BatchSize(size = 50)
	@JoinTable(name = "book_genre",
			joinColumns = @JoinColumn(name = "book_id"),
			inverseJoinColumns = @JoinColumn(name = "genre_id"))
	private Set<Genre> genres = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private UserAccount owner;

	@Column(precision = 10, scale = 2)
	private BigDecimal price;

	// What the shop paid for the book. Internal - customers must
	// never see the margin. THE reason DTOs exist (tutorial 07).
	@Column(precision = 10, scale = 2)
	private BigDecimal costPrice;

	protected Book() {
		// for JPA only
	}

	public Book(String title, Author author, BigDecimal price) {
		this.title = title;
		this.author = author;
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Set<Genre> getGenres() {
		return genres;
	}

	public void setGenres(Set<Genre> genres) {
		this.genres = genres;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public UserAccount getOwner() {
		return owner;
	}

	public void setOwner(UserAccount owner) {
		this.owner = owner;
	}

	public BigDecimal getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(BigDecimal costPrice) {
		this.costPrice = costPrice;
	}
}
