package com.keepsake.backend.challenge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeQuestionRepository extends JpaRepository<ChallengeQuestion, Long> {

    List<ChallengeQuestion> findAllByOrderByDayNumberAsc();

    Optional<ChallengeQuestion> findByDayNumber(int dayNumber);

    boolean existsByDayNumber(int dayNumber);
}
