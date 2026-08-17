package com.keepsake.backend.game;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TriviaQuestionRepository extends JpaRepository<TriviaQuestion, Long> {

    /** Active questions for a school — school-scoped plus global (school null) ones. */
    @Query("select q from TriviaQuestion q where q.active = true and (q.school.id = :schoolId or q.school is null)")
    List<TriviaQuestion> findPlayable(@Param("schoolId") Long schoolId);

    List<TriviaQuestion> findAllByOrderByCreatedAtDesc();
}
