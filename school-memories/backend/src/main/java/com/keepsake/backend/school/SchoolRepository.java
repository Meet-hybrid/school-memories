package com.keepsake.backend.school;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    java.util.Optional<School> findByNameIgnoreCase(String name);
}
