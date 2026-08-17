package com.keepsake.backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.achievement.AchievementService;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.user.User;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AchievementService achievementService;

    public NotificationService(NotificationRepository notificationRepository,
                               AchievementService achievementService) {
        this.notificationRepository = notificationRepository;
        this.achievementService = achievementService;
    }

    public void notifyLiked(Memory memory, User actor) {
        if (!memory.getUser().getId().equals(actor.getId())) {
            notify(memory.getUser(), actor, "LIKE",
                    actor.getNickname() != null ? actor.getNickname() : actor.getFullName()
                            + " liked your memory from Day " + memory.getDayNumber(),
                    memory.getId());
        }
    }

    public void notifyCommented(Memory memory, User actor) {
        if (!memory.getUser().getId().equals(actor.getId())) {
            notify(memory.getUser(), actor, "COMMENT",
                    actor.getNickname() != null ? actor.getNickname() : actor.getFullName()
                            + " commented on your memory from Day " + memory.getDayNumber(),
                    memory.getId());
        }
    }

    public void notifyFollowed(User recipient, User actor) {
        if (!recipient.getId().equals(actor.getId())) {
            notify(recipient, actor, "FOLLOW",
                    actor.getNickname() != null ? actor.getNickname() : actor.getFullName()
                            + " started following you",
                    null);
        }
    }

    /** Called after a memory is created — checks and notifies about new achievements. */
    public void notifyAchievements(User user) {
        achievementService.checkAndUnlock(user).forEach(code -> {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("ACHIEVEMENT");
            n.setMessage("Achievement unlocked: " + code);
            n.setRead(false);
            notificationRepository.save(n);
        });
    }

    public void notifyAnnouncement(User user, String title) {
        notify(user, null, "ANNOUNCEMENT", title, null);
    }

    private void notify(User recipient, User actor, String type, String message, Long memoryId) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setMessage(message);
        n.setMemoryId(memoryId);
        n.setRead(false);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> source = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(source.map(NotificationDto::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.keepsake.backend.common.ApiException(404, "Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new com.keepsake.backend.common.ApiException(403, "Not your notification");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }
}
