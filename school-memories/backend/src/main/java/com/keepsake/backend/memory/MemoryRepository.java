package com.keepsake.backend.memory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    @EntityGraph(attributePaths = {"user", "question"})
    Optional<Memory> findWithUserById(Long id);

    Optional<Memory> findByUserIdAndDayNumber(Long userId, int dayNumber);

    long countByUserIdAndDeletedFalse(Long userId);

    @Query("""
            select m from Memory m
            where m.deleted = false
              and (:userId is null or m.user.id = :userId)
              and (:schoolId is null or m.user.school.id = :schoolId)
              and (:dayNumber is null or m.dayNumber = :dayNumber)
            """)
    @EntityGraph(attributePaths = {"user", "question"})
    Page<Memory> feed(@Param("userId") Long userId, @Param("schoolId") Long schoolId,
                      @Param("dayNumber") Integer dayNumber, Pageable pageable);

    @Query("select m from Memory m where m.deleted = false and m.user.id = :userId order by m.dayNumber asc")
    List<Memory> findAllByUserIdOrderByDay(@Param("userId") Long userId);

    /** Total likes received by a user across all their memories. */
    @Query("select count(r) from Reaction r where r.memory.user.id = :userId and r.memory.deleted = false")
    long countLikesReceived(@Param("userId") Long userId);
}
