
package com.litloop.litloop.controller;

import com.litloop.litloop.entity.Book;
import com.litloop.litloop.entity.ReadingList;
import com.litloop.litloop.entity.ReadingStatus;
import com.litloop.litloop.entity.User;
import com.litloop.litloop.repository.BookRepository;
import com.litloop.litloop.repository.ReadingListRepository;
import com.litloop.litloop.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
public class ReadingListController {

    private final ReadingListRepository libraryRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public ReadingListController(
            ReadingListRepository libraryRepository,
            UserRepository userRepository,
            BookRepository bookRepository) {
        this.libraryRepository = libraryRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(
            authentication.getName().toLowerCase()
        ).orElseThrow(() ->
            new IllegalArgumentException("User not found")
        );
    }

    private Map<String, Object> toResponse(ReadingList entry) {
        Book book = entry.getBook();

        return Map.of(
            "id", entry.getId(),
            "bookId", book.getId(),
            "title", book.getTitle(),
            "author", book.getAuthor(),
            "genre", book.getGenre() == null ? "" : book.getGenre(),
            "coverImage", book.getCoverImage() == null
                ? "" : book.getCoverImage(),
            "status", entry.getStatus().name(),
            "progress", entry.getProgress()
        );
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getLibrary(
            Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<Map<String, Object>> items =
            libraryRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(items);
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<?> addBook(
            @PathVariable Long bookId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        if (libraryRepository.findByUserIdAndBookId(
                user.getId(), bookId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message",
                    "This book is already in your library."));
        }

        Book book = bookRepository.findById(bookId)
            .orElse(null);

        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Book not found."));
        }

        ReadingList entry = new ReadingList();
        entry.setUser(user);
        entry.setBook(book);
        entry.setStatus(ReadingStatus.WANT_TO_READ);
        entry.setProgress(0);

        ReadingList saved = libraryRepository.save(entry);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(saved));
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long bookId,
            @RequestBody UpdateRequest request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        ReadingList entry =
            libraryRepository.findByUserIdAndBookId(
                user.getId(), bookId
            ).orElse(null);

        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message",
                    "Book not found in your library."));
        }

        if (request.status() != null) {
            entry.setStatus(request.status());
        }

        if (request.progress() != null) {
            if (request.progress() < 0 ||
                    request.progress() > 100) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message",
                        "Progress must be between 0 and 100."));
            }
            entry.setProgress(request.progress());
        }

        if (entry.getStatus() == ReadingStatus.COMPLETED) {
            entry.setProgress(100);
        }

        ReadingList saved = libraryRepository.save(entry);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<?> removeBook(
            @PathVariable Long bookId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        ReadingList entry =
            libraryRepository.findByUserIdAndBookId(
                user.getId(), bookId
            ).orElse(null);

        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message",
                    "Book not found in your library."));
        }

        libraryRepository.delete(entry);

        return ResponseEntity.noContent().build();
    }

    public record UpdateRequest(
        ReadingStatus status,
        Integer progress
    ) {}
}
