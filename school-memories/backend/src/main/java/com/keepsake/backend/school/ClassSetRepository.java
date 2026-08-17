package com.keepsake.backend.school;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSetRepository extends JpaRepository<ClassSet, Long> {

    List<ClassSet> findBySchoolIdOrderByGraduationYearAscNameAsc(Long schoolId);
}
