package com.keepsake.backend.challenge;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.memory.MemoryDto;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.memory.MemoryService;
import com.keepsake.backend.user.UserRepository;

@RestController
@RequestMapping("/api/challenge")
public class ChallengeController {

    private final ChallengeQuestionRepository questionRepository;
    private final MemoryRepository memoryRepository;
    private final MemoryService memoryService;
    private final UserRepository userRepository;

    public ChallengeController(ChallengeQuestionRepository questionRepository,
                               MemoryRepository memoryRepository,
                               MemoryService memoryService,
                               UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.memoryRepository = memoryRepository;
        this.memoryService = memoryService;
        this.userRepository = userRepository;
    }

    /**
     * The full 30-day timeline for the current user: every question plus
     * completion status and a snippet of the user's answer where answered.
     */
    @GetMapping
    public Map<String, Object> timeline(Authentication auth) {
        Long userId = Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
        List<ChallengeQuestion> questions = questionRepository.findAllByOrderByDayNumberAsc();
        List<Memory> myMemories = memoryRepository.findAllByUserIdOrderByDay(userId);
        Map<Integer, Memory> byDay = myMemories.stream()
                .collect(Collectors.toMap(Memory::getDayNumber, Function.identity()));

        List<DayDto> days = questions.stream()
                .map(q -> {
                    Memory m = byDay.get(q.getDayNumber());
                    return new DayDto(
                            q.getDayNumber(),
                            q.getQuestion(),
                            q.getHint(),
                            q.isActive(),
                            m != null,
                            m != null ? m.getId() : null,
                            m != null ? m.getAnswer() : null,
                            m != null ? m.getCreatedAt() : null);
                })
                .toList();

        long answered = days.stream().filter(DayDto::answered).count();
        long streak = com.keepsake.backend.achievement.AchievementService.longestStreak(myMemories);
        return Map.of("days", days, "answeredCount", answered, "total", questions.size(), "streak", streak);
    }

    /** One day's question + the user's memory for it (if any). */
    @GetMapping("/day/{day}")
    public DayDetail day(@PathVariable int day, Authentication auth) {
        ChallengeQuestion question = questionRepository.findByDayNumber(day)
                .orElseThrow(() -> ApiException.notFound("No question for day " + day));
        Long userId = Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
        MemoryDto memory = memoryRepository.findByUserIdAndDayNumber(userId, day)
                .map(m -> memoryService.get(m.getId(), userId))
                .orElse(null);
        return new DayDetail(
                question.getDayNumber(),
                question.getQuestion(),
                question.getHint(),
                question.isActive(),
                memory);
    }

    public record DayDto(int dayNumber, String question, String hint, boolean active,
                         boolean answered, Long memoryId, String answerSnippet,
                         java.time.LocalDateTime answeredAt) {
    }

    public record DayDetail(int dayNumber, String question, String hint, boolean active, MemoryDto memory) {
    }
}
