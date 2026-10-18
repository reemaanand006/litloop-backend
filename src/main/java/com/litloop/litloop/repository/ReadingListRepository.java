
package com.litloop.litloop.repository;

import com.litloop.litloop.entity.ReadingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingListRepository
        extends JpaRepository<ReadingList, Long> {

    List<ReadingList> findByUserId(Long userId);

    Optional<ReadingList> findByUserIdAndBookId(
        Long userId,
        Long bookId
    );
}
