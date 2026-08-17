package com.keepsake.backend.notification;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keepsake.backend.common.PageResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationDto> list(Authentication auth,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "30") int size) {
        return notificationService.list(currentUserId(auth), page, Math.min(size, 100));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationService.unreadCount(currentUserId(auth)));
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markRead(currentUserId(auth), id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(Authentication auth) {
        notificationService.markAllRead(currentUserId(auth));
    }

    private Long currentUserId(Authentication auth) {
        return Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
    }
}
