package com.keepsake.backend.memory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.keepsake.backend.user.User;

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

    /** Classmates' non-deleted memories (not the viewer's), newest first — Guess Who pool. */
    @Query("""
            select m from Memory m
            where m.deleted = false and m.user.id <> :selfId and m.user.school.id = :schoolId
              and length(m.answer) >= 40
            order by m.createdAt desc
            """)
    List<Memory> findGuessPool(@Param("selfId") Long selfId, @Param("schoolId") Long schoolId, Pageable pageable);

    /** A classmate who has posted a photo, newest memory first. */
    @Query("""
            select m.user from Memory m
            where m.deleted = false and m.mediaUrl is not null and m.user.id <> :selfId
              and m.user.school.id = :schoolId
            order by m.createdAt desc
            """)
    List<User> findPhotoAuthorOfClassmate(@Param("selfId") Long selfId, @Param("schoolId") Long schoolId, Pageable pageable);

    /** Classmates with at least {@code min} memories, most prolific first. */
    @Query("""
            select m.user from Memory m
            where m.deleted = false and m.user.id <> :selfId and m.user.school.id = :schoolId
            group by m.user having count(m) >= :min
            order by count(m) desc
            """)
    List<User> findProlificClassmates(@Param("selfId") Long selfId, @Param("schoolId") Long schoolId,
                                      @Param("min") long min, Pageable pageable);
}
