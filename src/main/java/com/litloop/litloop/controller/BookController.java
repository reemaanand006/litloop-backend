
package com.litloop.litloop.controller;

import com.litloop.litloop.entity.Book;
import com.litloop.litloop.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:5173")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Get all books
    @GetMapping
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // Get a book by ID
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found"));
    }

    // Add a new book
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book addBook(@RequestBody Book book) {
        book.setId(null);
        return bookRepository.save(book);
    }

    // Delete a book
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Book not found");
        }

        bookRepository.deleteById(id);
    }
}
