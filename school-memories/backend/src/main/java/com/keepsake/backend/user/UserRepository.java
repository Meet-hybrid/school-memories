package com.keepsake.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where u.active = true and u.school.id = :schoolId
              and (:q is null or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.nickname, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.username, '')) like lower(concat('%', :q, '%')))
              and (:setId is null or u.classSet.id = :setId)
              and (:year is null or u.graduationYear = :year)
            """)
    Page<User> search(
            @Param("schoolId") Long schoolId,
            @Param("q") String q,
            @Param("setId") Long setId,
            @Param("year") Integer year,
            Pageable pageable);

    /** Suggested classmates: same school, not self, not yet followed, ordered by memory count. */
    @Query("""
            select u from User u
            where u.active = true and u.id <> :selfId
              and u.school.id = :schoolId
              and u.id not in (select f.following.id from Follow f where f.follower.id = :selfId)
            order by (select count(m) from Memory m where m.user.id = u.id and m.deleted = false) desc
            """)
    List<User> findSuggested(@Param("schoolId") Long schoolId, @Param("selfId") Long selfId, Pageable pageable);
}
