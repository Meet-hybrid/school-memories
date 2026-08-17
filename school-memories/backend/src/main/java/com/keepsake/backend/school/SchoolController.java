package com.keepsake.backend.school;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;

    public SchoolController(SchoolRepository schoolRepository, ClassSetRepository classSetRepository) {
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
    }

    @GetMapping
    public List<SchoolDto> list() {
        return schoolRepository.findByActiveTrueOrderByNameAsc().stream().map(SchoolDto::from).toList();
    }

    @GetMapping("/{schoolId}/sets")
    public List<SetDto> sets(@PathVariable Long schoolId) {
        return classSetRepository.findBySchoolIdOrderByGraduationYearAscNameAsc(schoolId).stream()
                .map(SetDto::from).toList();
    }

    public record SchoolDto(Long id, String name, String description) {
        static SchoolDto from(School s) {
            return new SchoolDto(s.getId(), s.getName(), s.getDescription());
        }
    }

    public record SetDto(Long id, String name, Integer graduationYear) {
        static SetDto from(ClassSet cs) {
            return new SetDto(cs.getId(), cs.getName(), cs.getGraduationYear());
        }
    }
}
