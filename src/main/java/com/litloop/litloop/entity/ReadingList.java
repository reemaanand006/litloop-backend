
package com.litloop.litloop.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "reading_list",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"user_id", "book_id"}
        )
    }
)
public class ReadingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus status = ReadingStatus.WANT_TO_READ;

    @Column(nullable = false)
    private int progress = 0;

    public ReadingList() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException(
                "Progress must be between 0 and 100"
            );
        }
        this.progress = progress;
    }
}
