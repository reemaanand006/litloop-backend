
package com.litloop.litloop.repository;

import com.litloop.litloop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    long countByBookId(Long bookId);
}
