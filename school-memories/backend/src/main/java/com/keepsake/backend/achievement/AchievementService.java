package com.keepsake.backend.achievement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.user.User;

@Service
public class AchievementService {

    /** Codes are stable identifiers referenced by the frontend for icons/ordering. */
    public static final List<AchievementDef> DEFINITIONS = List.of(
            new AchievementDef("FIRST_MEMORY", "First Memory", "You shared your first school memory."),
            new AchievementDef("STORYTELLER", "Storyteller", "You've shared 10 memories."),
            new AchievementDef("MEMORY_KEEPER", "Memory Keeper", "You've shared 20 memories."),
            new AchievementDef("STREAK_7", "7-Day Streak", "You answered on 7 consecutive days."),
            new AchievementDef("STREAK_15", "15-Day Streak", "You answered on 15 consecutive days."),
            new AchievementDef("PHOTOGRAPHER", "Photographer", "You added a photo to a memory."),
            new AchievementDef("COMMUNITY_FAVOURITE", "Community Favourite", "Your memories earned 10 likes."),
            new AchievementDef("CHALLENGE_COMPLETE", "Challenge Complete", "You answered all 30 questions."),
            new AchievementDef("HALL_OF_FAME", "Hall of Fame", "You finished the challenge and earned 25 likes."));

    public record AchievementDef(String code, String name, String description) {
    }

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final MemoryRepository memoryRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              MemoryRepository memoryRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.memoryRepository = memoryRepository;
    }

    /** Idempotent — seeds the achievement definitions used by the app. */
    @Transactional
    public void seedDefinitions() {
        for (AchievementDef def : DEFINITIONS) {
            if (achievementRepository.findByCode(def.code()).isEmpty()) {
                Achievement a = new Achievement();
                a.setCode(def.code());
                a.setName(def.name());
                a.setDescription(def.description());
                achievementRepository.save(a);
            }
        }
    }

    /**
     * Evaluates a user's stats against every achievement and unlocks any that
     * are newly earned. Returns the codes unlocked (used for notifications).
     */
    @Transactional
    public List<String> checkAndUnlock(User user) {
        seedDefinitions();
        List<Memory> memories = memoryRepository.findAllByUserIdOrderByDay(user.getId());
        long count = memories.size();
        long likesReceived = memoryRepository.countLikesReceived(user.getId());
        boolean hasPhoto = memories.stream().anyMatch(m -> "PHOTO".equals(m.getMediaType()));
        long streak = longestStreak(memories);
        boolean complete = count >= 30;

        List<String> unlocked = new ArrayList<>();
        grant(user, "FIRST_MEMORY", count >= 1, unlocked);
        grant(user, "STORYTELLER", count >= 10, unlocked);
        grant(user, "MEMORY_KEEPER", count >= 20, unlocked);
        grant(user, "STREAK_7", streak >= 7, unlocked);
        grant(user, "STREAK_15", streak >= 15, unlocked);
        grant(user, "PHOTOGRAPHER", hasPhoto, unlocked);
        grant(user, "COMMUNITY_FAVOURITE", likesReceived >= 10, unlocked);
        grant(user, "CHALLENGE_COMPLETE", complete, unlocked);
        grant(user, "HALL_OF_FAME", complete && likesReceived >= 25, unlocked);
        return unlocked;
    }

    private void grant(User user, String code, boolean earned, List<String> unlocked) {
        if (!earned) {
            return;
        }
        Achievement achievement = achievementRepository.findByCode(code).orElse(null);
        if (achievement == null) {
            return;
        }
        if (!userAchievementRepository.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepository.save(ua);
            unlocked.add(achievement.getName());
        }
    }

    @Transactional(readOnly = true)
    public List<UserAchievementDto> forUser(Long userId) {
        return userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(userId).stream()
                .map(ua -> new UserAchievementDto(ua.getAchievement().getCode(), ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(), ua.getUnlockedAt()))
                .toList();
    }

    /** Longest run of consecutive days (by calendar date) on which the user answered. */
    public static long longestStreak(List<Memory> memories) {
        Set<LocalDate> days = new LinkedHashSet<>();
        memories.forEach(m -> days.add(m.getCreatedAt().toLocalDate()));
        if (days.isEmpty()) {
            return 0;
        }
        List<LocalDate> sorted = new ArrayList<>(days).stream().sorted().toList();
        long best = 1;
        long current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).minusDays(1).equals(sorted.get(i - 1))) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }
        return best;
    }

    public record UserAchievementDto(String code, String name, String description, java.time.LocalDateTime unlockedAt) {
    }
}
