package com.keepsake.backend.achievement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.memory.CommentRepository;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.user.Role;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

/**
 * Community leaderboards. Deliberately quiet: they reward participation and
 * storytelling, never speed or "winning".
 */
@Service
public class LeaderboardService {

    private final UserRepository userRepository;
    private final MemoryRepository memoryRepository;
    private final CommentRepository commentRepository;

    public LeaderboardService(UserRepository userRepository, MemoryRepository memoryRepository,
                              CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.memoryRepository = memoryRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<Entry> top(String type, int limit) {
        int n = Math.min(limit, 50);
        List<Entry> entries = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            // leaderboards celebrate classmates, not staff accounts
            if (!u.isActive() || u.getSchool() == null || u.getRole() == Role.ADMIN) {
                continue;
            }
            long value = switch (type) {
                case "memories" -> memoryRepository.countByUserIdAndDeletedFalse(u.getId());
                case "likes" -> memoryRepository.countLikesReceived(u.getId());
                case "comments" -> commentRepository.countByUserId(u.getId());
                case "streak" -> AchievementService.longestStreak(memoryRepository.findAllByUserIdOrderByDay(u.getId()));
                default -> 0;
            };
            entries.add(new Entry(u.getId(), displayName(u), u.getAvatarUrl(), value));
        }
        entries.sort((a, b) -> Long.compare(b.value(), a.value()));
        return entries.size() > n ? entries.subList(0, n) : entries;
    }

    private static String displayName(User u) {
        return u.getNickname() != null ? u.getNickname() : u.getFullName();
    }

    public record Entry(Long userId, String name, String avatarUrl, long value) {
    }
}
