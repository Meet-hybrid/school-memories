package com.keepsake.backend.user;

import java.time.LocalDateTime;

import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.School;

/** Public-facing representation of a user profile. */
public record UserDto(
        Long id,
        String email,
        String fullName,
        String nickname,
        String username,
        String bio,
        String avatarUrl,
        SchoolRef school,
        SetRef classSet,
        Integer graduationYear,
        String role,
        boolean verified,
        boolean following,
        long followers,
        long followingCount,
        long memories,
        long likesReceived,
        LocalDateTime createdAt) {

    public record SchoolRef(Long id, String name) {
    }

    public record SetRef(Long id, String name, Integer graduationYear) {
    }

    public static UserDto from(User u, boolean following, long followers, long followingCount,
                               long memories, long likesReceived) {
        School school = u.getSchool();
        ClassSet set = u.getClassSet();
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getNickname(),
                u.getUsername(),
                u.getBio(),
                u.getAvatarUrl(),
                school != null ? new SchoolRef(school.getId(), school.getName()) : null,
                set != null ? new SetRef(set.getId(), set.getName(), set.getGraduationYear()) : null,
                u.getGraduationYear(),
                u.getRole().name(),
                u.isVerified(),
                following,
                followers,
                followingCount,
                memories,
                likesReceived,
                u.getCreatedAt());
    }
}
