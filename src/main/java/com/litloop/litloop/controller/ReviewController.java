package com.litloop.litloop.controller;

import com.litloop.litloop.entity.Book;
import com.litloop.litloop.entity.ReadingList;
import com.litloop.litloop.entity.ReadingStatus;
import com.litloop.litloop.entity.Review;
import com.litloop.litloop.entity.User;
import com.litloop.litloop.repository.BookRepository;
import com.litloop.litloop.repository.ReadingListRepository;
import com.litloop.litloop.repository.ReviewRepository;
import com.litloop.litloop.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReadingListRepository readingListRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            ReadingListRepository readingListRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.readingListRepository = readingListRepository;
    }

    @GetMapping("/book/{bookId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBookRating(
            @PathVariable Long bookId,
            Authentication authentication) {

        if (!bookRepository.existsById(bookId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Book not found."));
        }

        List<Review> ratings =
                reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);

        double average = ratings.isEmpty()
                ? 0.0
                : Math.round(
                        ratings.stream()
                                .mapToInt(Review::getRating)
                                .average()
                                .orElse(0.0) * 10.0
                ) / 10.0;

        Long myRating = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            User user = userRepository
                    .findByEmail(authentication.getName().toLowerCase())
                    .orElse(null);

            if (user != null) {
                myRating = reviewRepository
                        .findByBookIdAndUserId(bookId, user.getId())
                        .map(review -> review.getRating().longValue())
                        .orElse(null);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", bookId);
        response.put("averageRating", average);
        response.put("ratingCount", ratings.size());
        response.put("reviewCount", ratings.size());
        response.put("myRating", myRating);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/book/{bookId}")
    @Transactional
    public ResponseEntity<?> saveRating(
            @PathVariable Long bookId,
            @RequestBody RatingRequest request,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in to rate a book."));
        }

        if (request == null
                || request.rating() == null
                || request.rating() < 1
                || request.rating() > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Rating must be between 1 and 5."));
        }

        User user = userRepository
                .findByEmail(authentication.getName().toLowerCase())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User account not found."));
        }

        Book book = bookRepository.findById(bookId).orElse(null);

        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Book not found."));
        }

        ReadingList entry = readingListRepository
                .findByUserIdAndBookId(user.getId(), bookId)
                .orElse(null);

        if (entry == null
                || entry.getStatus() != ReadingStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can rate a book only after marking it Completed."
                    ));
        }

        Review review = reviewRepository
                .findByBookIdAndUserId(bookId, user.getId())
                .orElseGet(Review::new);

        review.setUser(user);
        review.setBook(book);
        review.setRating(request.rating());

        // The existing database column is non-null.
        // Written reviews are no longer part of this feature.
        review.setText("");

        Review saved = reviewRepository.save(review);

        return ResponseEntity.ok(Map.of(
                "bookId", bookId,
                "rating", saved.getRating(),
                "message", "Rating saved successfully."
        ));
    }

    public record RatingRequest(Integer rating) {
    }
}
