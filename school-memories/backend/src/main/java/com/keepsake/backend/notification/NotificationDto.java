package com.keepsake.backend.notification;

import java.time.LocalDateTime;

import com.keepsake.backend.user.User;

public record NotificationDto(
        Long id,
        String type,
        String message,
        Long actorId,
        String actorName,
        String actorAvatarUrl,
        Long memoryId,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationDto from(Notification n) {
        User actor = n.getActor();
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getMessage(),
                actor != null ? actor.getId() : null,
                actor != null ? (actor.getNickname() != null ? actor.getNickname() : actor.getFullName()) : null,
                actor != null ? actor.getAvatarUrl() : null,
                n.getMemoryId(),
                n.isRead(),
                n.getCreatedAt());
    }
}
