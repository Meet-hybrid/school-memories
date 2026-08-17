package com.keepsake.backend.game;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.keepsake.backend.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A user's 5x5 classmate-bingo card. {@code rules} holds 25 rule codes in
 * row-major order; {@code completed} tracks which rules have been verified.
 * Cards are regenerated on request (dead cards happen when rules can't be met).
 */
@Entity
@Table(name = "bingo_card")
public class BingoCard {

    public static final int SIZE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bingo_rule", joinColumns = @JoinColumn(name = "bingo_card_id"))
    @Column(name = "rule", length = 30)
    private List<String> rules = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bingo_completed_rule", joinColumns = @JoinColumn(name = "bingo_card_id"))
    @Column(name = "rule", length = 30)
    private Set<String> completed = new HashSet<>();

    @Column(name = "bingo_claimed", nullable = false)
    private boolean bingoClaimed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
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

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public Set<String> getCompleted() {
        return completed;
    }

    public boolean isBingoClaimed() {
        return bingoClaimed;
    }

    public void setBingoClaimed(boolean bingoClaimed) {
        this.bingoClaimed = bingoClaimed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
