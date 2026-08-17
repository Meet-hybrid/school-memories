package com.keepsake.backend.admin;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import com.keepsake.backend.admin.AdminService.CommentRow;
import com.keepsake.backend.admin.AdminService.MemoryRow;
import com.keepsake.backend.admin.AdminService.QuestionRow;
import com.keepsake.backend.admin.AdminService.TriviaRow;
import com.keepsake.backend.admin.AdminService.UserRow;
import com.keepsake.backend.announcement.Announcement;
import com.keepsake.backend.challenge.ChallengeQuestion;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.School;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.stats();
    }

    // ----- users -----

    @GetMapping("/users")
    public PageResponse<UserRow> users(@RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return adminService.users(q, page, Math.min(size, 100));
    }

    @PatchMapping("/users/{id}/active")
    public void setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        adminService.setUserActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @PatchMapping("/users/{id}/role")
    public void setRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.setUserRole(id, body.get("role"));
    }

    // ----- schools & sets -----

    @PostMapping("/schools")
    public School createSchool(@Valid @RequestBody SchoolRequest req) {
        return adminService.createSchool(req.name(), req.description());
    }

    @PatchMapping("/schools/{id}")
    public School updateSchool(@PathVariable Long id, @RequestBody SchoolRequest req) {
        return adminService.updateSchool(id, req.name(), req.description(), req.active());
    }

    @PostMapping("/schools/{id}/sets")
    public ClassSet createSet(@PathVariable Long id, @Valid @RequestBody SetRequest req) {
        return adminService.createSet(id, req.name(), req.graduationYear());
    }

    // ----- questions -----

    @GetMapping("/questions")
    public List<QuestionRow> questions() {
        return adminService.questions();
    }

    @PostMapping("/questions")
    public ChallengeQuestion createQuestion(@Valid @RequestBody QuestionRequest req) {
        return adminService.createQuestion(req.dayNumber(), req.question(), req.hint());
    }

    @PatchMapping("/questions/{id}")
    public ChallengeQuestion updateQuestion(@PathVariable Long id, @RequestBody QuestionRequest req) {
        return adminService.updateQuestion(id, req.dayNumber(), req.question(), req.hint(), req.active());
    }

    // ----- moderation -----

    @GetMapping("/memories")
    public PageResponse<MemoryRow> memories(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminService.memories(page, Math.min(size, 100));
    }

    @PatchMapping("/memories/{id}/moderate")
    public void moderateMemory(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        adminService.setMemoryDeleted(id, Boolean.TRUE.equals(body.get("deleted")));
    }

    @GetMapping("/comments")
    public PageResponse<CommentRow> comments(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return adminService.comments(page, Math.min(size, 100));
    }

    @DeleteMapping("/comments/{id}")
    public void deleteComment(@PathVariable Long id) {
        adminService.deleteComment(id);
    }

    // ----- games: trivia -----

    @GetMapping("/games/trivia")
    public List<TriviaRow> trivia() {
        return adminService.trivia();
    }

    @PostMapping("/games/trivia")
    public TriviaRow createTrivia(Authentication auth, @Valid @RequestBody TriviaRequest req) {
        return adminService.createTrivia(currentUserId(auth), req.question(), req.options(), req.correctIndex());
    }

    @PatchMapping("/games/trivia/{id}")
    public TriviaRow updateTrivia(@PathVariable Long id, @RequestBody TriviaUpdateRequest req) {
        return adminService.updateTrivia(id, req.active());
    }

    @DeleteMapping("/games/trivia/{id}")
    public void deleteTrivia(@PathVariable Long id) {
        adminService.deleteTrivia(id);
    }

    // ----- announcements -----

    @GetMapping("/announcements")
    public List<Announcement> announcements() {
        return adminService.announcements();
    }

    @PostMapping("/announcements")
    public Announcement createAnnouncement(@Valid @RequestBody AnnouncementRequest req) {
        return adminService.createAnnouncement(req.title(), req.body());
    }

    @DeleteMapping("/announcements/{id}")
    public void deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
    }

    // ----- DTOs -----

    public record SchoolRequest(String name, String description, Boolean active) {
    }

    public record SetRequest(@NotBlank String name, Integer graduationYear) {
    }

    public record QuestionRequest(@NotNull Integer dayNumber, @NotBlank String question, String hint, Boolean active) {
    }

    public record AnnouncementRequest(@NotBlank String title, @NotBlank String body) {
    }

    public record TriviaRequest(@NotBlank String question,
                                @NotNull List<@NotBlank String> options,
                                @NotNull @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(3) Integer correctIndex) {
    }

    public record TriviaUpdateRequest(Boolean active) {
    }

    private Long currentUserId(Authentication auth) {
        return Long.valueOf(((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername());
    }
}
