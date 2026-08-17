package com.keepsake.backend.game;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BingoCardRepository extends JpaRepository<BingoCard, Long> {

    Optional<BingoCard> findByUserId(Long userId);
}
