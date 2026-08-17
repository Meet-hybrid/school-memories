package com.keepsake.backend.achievement;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keepsake.backend.achievement.AchievementService.UserAchievementDto;

@RestController
@RequestMapping("/api")
public class AchievementController {

    private final AchievementService achievementService;
    private final LeaderboardService leaderboardService;

    public AchievementController(AchievementService achievementService, LeaderboardService leaderboardService) {
        this.achievementService = achievementService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/achievements/me")
    public List<UserAchievementDto> myAchievements(Authentication auth) {
        return achievementService.forUser(currentUserId(auth));
    }

    @GetMapping("/leaderboards")
    public List<LeaderboardService.Entry> leaderboard(@RequestParam(defaultValue = "memories") String type,
                                                      @RequestParam(defaultValue = "10") int limit) {
        if (!List.of("memories", "likes", "comments", "streak", "games").contains(type)) {
            type = "memories";
        }
        return leaderboardService.top(type, limit);
    }

    private Long currentUserId(Authentication auth) {
        return Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
    }
}
