package com.keepsake.backend.game;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    Optional<GamePlayer> findByUserId(Long userId);
}
