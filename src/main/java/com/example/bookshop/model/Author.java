package com.example.bookshop.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Author entity with the inverse side of the book relationship.
 *
 * | Key                    | Why we use it                                      |
 * |------------------------|----------------------------------------------------|
 * | @Entity                | Maps authors to database rows                     |
 * | @Id / @GeneratedValue  | Marks the database-generated identifier             |
 * | @OneToMany             | Represents one author having many books             |
 * | mappedBy = "author"   | Points to Book.author, the owning foreign-key side  |
 * | protected constructor  | Allows JPA to create an entity from a database row |
 */
@Entity
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();

    protected Author() {
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
