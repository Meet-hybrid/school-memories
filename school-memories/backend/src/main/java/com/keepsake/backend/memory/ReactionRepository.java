package com.keepsake.backend.memory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByMemoryIdAndUserId(Long memoryId, Long userId);

    long countByMemoryId(Long memoryId);

    long countByMemoryIdAndType(Long memoryId, String type);
}
