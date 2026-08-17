package com.keepsake.backend.memory;

import java.time.LocalDateTime;

import com.keepsake.backend.user.User;

public record CommentDto(
        Long id,
        Long authorId,
        String authorName,
        String authorNickname,
        String authorUsername,
        String authorAvatarUrl,
        String body,
        LocalDateTime createdAt) {

    public static CommentDto from(Comment c) {
        User u = c.getUser();
        return new CommentDto(
                c.getId(),
                u.getId(),
                u.getFullName(),
                u.getNickname(),
                u.getUsername(),
                u.getAvatarUrl(),
                c.getBody(),
                c.getCreatedAt());
    }
}
