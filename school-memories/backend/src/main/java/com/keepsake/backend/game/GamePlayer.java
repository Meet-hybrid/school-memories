package com.keepsake.backend.game;

import java.time.LocalDateTime;

import com.keepsake.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Cumulative game scores. Points: guessWhoCorrect + triviaCorrect + bingosCompleted * 5. */
@Entity
@Table(name = "game_player")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "guess_who_correct", nullable = false)
    private int guessWhoCorrect;

    @Column(name = "trivia_correct", nullable = false)
    private int triviaCorrect;

    @Column(name = "bingos_completed", nullable = false)
    private int bingosCompleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getGuessWhoCorrect() {
        return guessWhoCorrect;
    }

    public void setGuessWhoCorrect(int guessWhoCorrect) {
        this.guessWhoCorrect = guessWhoCorrect;
    }

    public int getTriviaCorrect() {
        return triviaCorrect;
    }

    public void setTriviaCorrect(int triviaCorrect) {
        this.triviaCorrect = triviaCorrect;
    }

    public int getBingosCompleted() {
        return bingosCompleted;
    }

    public void setBingosCompleted(int bingosCompleted) {
        this.bingosCompleted = bingosCompleted;
    }

    public int total() {
        return guessWhoCorrect + triviaCorrect + bingosCompleted * 5;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
