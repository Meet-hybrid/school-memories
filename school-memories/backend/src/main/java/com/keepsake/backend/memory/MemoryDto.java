package com.keepsake.backend.memory;

import java.time.LocalDateTime;

import com.keepsake.backend.challenge.ChallengeQuestion;
import com.keepsake.backend.user.User;

/** A memory post as seen in the feed / on a profile / in the challenge timeline. */
public record MemoryDto(
        Long id,
        Author author,
        int dayNumber,
        String question,
        String answer,
        String mood,
        String mediaUrl,
        String mediaType,
        long likes,
        long comments,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record Author(
            Long id, String fullName, String nickname, String username, String avatarUrl,
            Long schoolId, String schoolName, String className, Integer graduationYear) {
    }

    public static Author authorOf(User u) {
        return new Author(
                u.getId(), u.getFullName(), u.getNickname(), u.getUsername(), u.getAvatarUrl(),
                u.getSchool() != null ? u.getSchool().getId() : null,
                u.getSchool() != null ? u.getSchool().getName() : null,
                u.getClassSet() != null ? u.getClassSet().getName() : null,
                u.getGraduationYear());
    }

    public static MemoryDto from(Memory m, long likes, long comments, boolean likedByMe) {
        ChallengeQuestion q = m.getQuestion();
        return new MemoryDto(
                m.getId(),
                authorOf(m.getUser()),
                m.getDayNumber(),
                q != null ? q.getQuestion() : null,
                m.getAnswer(),
                m.getMood(),
                m.getMediaUrl(),
                m.getMediaType(),
                likes,
                comments,
                likedByMe,
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
