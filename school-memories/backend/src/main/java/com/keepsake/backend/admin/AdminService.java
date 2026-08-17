package com.keepsake.backend.admin;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.announcement.Announcement;
import com.keepsake.backend.announcement.AnnouncementRepository;
import com.keepsake.backend.challenge.ChallengeQuestion;
import com.keepsake.backend.challenge.ChallengeQuestionRepository;
import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.game.TriviaQuestion;
import com.keepsake.backend.game.TriviaQuestionRepository;
import com.keepsake.backend.memory.Comment;
import com.keepsake.backend.memory.CommentRepository;
import com.keepsake.backend.memory.Memory;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.ClassSetRepository;
import com.keepsake.backend.school.School;
import com.keepsake.backend.school.SchoolRepository;
import com.keepsake.backend.user.Role;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;
    private final ChallengeQuestionRepository questionRepository;
    private final MemoryRepository memoryRepository;
    private final CommentRepository commentRepository;
    private final AnnouncementRepository announcementRepository;
    private final TriviaQuestionRepository triviaQuestionRepository;

    public AdminService(UserRepository userRepository,
                        SchoolRepository schoolRepository,
                        ClassSetRepository classSetRepository,
                        ChallengeQuestionRepository questionRepository,
                        MemoryRepository memoryRepository,
                        CommentRepository commentRepository,
                        AnnouncementRepository announcementRepository,
                        TriviaQuestionRepository triviaQuestionRepository) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
        this.questionRepository = questionRepository;
        this.memoryRepository = memoryRepository;
        this.commentRepository = commentRepository;
        this.announcementRepository = announcementRepository;
        this.triviaQuestionRepository = triviaQuestionRepository;
    }

    // ----- stats -----

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long users = userRepository.count();
        long schools = schoolRepository.count();
        long memories = memoryRepository.count();
        long liveMemories = memoryRepository.count();
        return Map.of(
                "users", users,
                "schools", schools,
                "memories", memories,
                "comments", commentRepository.count(),
                "questions", questionRepository.count(),
                "announcements", announcementRepository.count());
    }

    // ----- users -----

    @Transactional(readOnly = true)
    public PageResponse<UserRow> users(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> source;
        if (q != null && !q.isBlank()) {
            source = userRepository.findAll(pageable); // filtered below for simplicity
            var filtered = source.getContent().stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(q.toLowerCase())
                            || u.getFullName().toLowerCase().contains(q.toLowerCase()))
                    .toList();
            return new PageResponse<>(filtered.stream().map(u -> UserRow.from(u, memoriesOf(u))).toList(), page, size,
                    filtered.size(), (int) Math.ceil(filtered.size() / (double) Math.max(size, 1)));
        }
        source = userRepository.findAll(pageable);
        return PageResponse.of(source.map(u -> UserRow.from(u, memoriesOf(u))));
    }

    private long memoriesOf(User u) {
        return memoryRepository.countByUserIdAndDeletedFalse(u.getId());
    }

    @Transactional
    public void setUserActive(Long userId, boolean active) {
        User u = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setActive(active);
        userRepository.save(u);
    }

    @Transactional
    public void setUserRole(Long userId, String role) {
        User u = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setRole(Role.valueOf(role));
        userRepository.save(u);
    }

    // ----- schools & sets -----

    @Transactional
    public School createSchool(String name, String description) {
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("School name is required");
        }
        if (schoolRepository.existsByNameIgnoreCase(name.trim())) {
            throw ApiException.conflict("A school with this name already exists");
        }
        School s = new School();
        s.setName(name.trim());
        s.setDescription(description);
        return schoolRepository.save(s);
    }

    @Transactional
    public School updateSchool(Long id, String name, String description, Boolean active) {
        School s = schoolRepository.findById(id).orElseThrow(() -> ApiException.notFound("School not found"));
        if (name != null && !name.isBlank()) {
            s.setName(name.trim());
        }
        if (description != null) {
            s.setDescription(description);
        }
        if (active != null) {
            s.setActive(active);
        }
        return schoolRepository.save(s);
    }

    @Transactional
    public ClassSet createSet(Long schoolId, String name, Integer graduationYear) {
        School s = schoolRepository.findById(schoolId).orElseThrow(() -> ApiException.notFound("School not found"));
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("Set name is required");
        }
        ClassSet cs = new ClassSet();
        cs.setSchool(s);
        cs.setName(name.trim());
        cs.setGraduationYear(graduationYear);
        return classSetRepository.save(cs);
    }

    // ----- questions -----

    @Transactional(readOnly = true)
    public List<QuestionRow> questions() {
        return questionRepository.findAllByOrderByDayNumberAsc().stream().map(QuestionRow::from).toList();
    }

    @Transactional
    public ChallengeQuestion createQuestion(int dayNumber, String question, String hint) {
        if (question == null || question.isBlank()) {
            throw ApiException.badRequest("Question text is required");
        }
        if (dayNumber < 1 || dayNumber > 365) {
            throw ApiException.badRequest("Day number must be between 1 and 365");
        }
        if (questionRepository.existsByDayNumber(dayNumber)) {
            throw ApiException.conflict("A question for day " + dayNumber + " already exists");
        }
        ChallengeQuestion q = new ChallengeQuestion();
        q.setDayNumber(dayNumber);
        q.setQuestion(question.trim());
        q.setHint(hint);
        return questionRepository.save(q);
    }

    @Transactional
    public ChallengeQuestion updateQuestion(Long id, Integer dayNumber, String question, String hint, Boolean active) {
        ChallengeQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        if (dayNumber != null && dayNumber != q.getDayNumber()) {
            if (questionRepository.existsByDayNumber(dayNumber)) {
                throw ApiException.conflict("A question for day " + dayNumber + " already exists");
            }
            q.setDayNumber(dayNumber);
        }
        if (question != null && !question.isBlank()) {
            q.setQuestion(question.trim());
        }
        if (hint != null) {
            q.setHint(hint);
        }
        if (active != null) {
            q.setActive(active);
        }
        return questionRepository.save(q);
    }

    // ----- moderation -----

    @Transactional(readOnly = true)
    public PageResponse<MemoryRow> memories(int page, int size) {
        return PageResponse.of(memoryRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(MemoryRow::from));
    }

    @Transactional
    public void setMemoryDeleted(Long id, boolean deleted) {
        Memory m = memoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Memory not found"));
        m.setDeleted(deleted);
        memoryRepository.save(m);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentRow> comments(int page, int size) {
        return PageResponse.of(commentRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(CommentRow::from));
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment c = commentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Comment not found"));
        c.setDeleted(true);
        commentRepository.save(c);
    }

    // ----- trivia -----

    @Transactional(readOnly = true)
    public List<TriviaRow> trivia() {
        return triviaQuestionRepository.findAllByOrderByCreatedAtDesc().stream().map(TriviaRow::from).toList();
    }

    @Transactional
    public TriviaRow createTrivia(Long adminId, String question, List<String> options, int correctIndex) {
        if (question == null || question.isBlank()) {
            throw ApiException.badRequest("Question text is required");
        }
        if (options == null || options.size() != 4 || options.stream().anyMatch(o -> o == null || o.isBlank())) {
            throw ApiException.badRequest("Provide exactly four non-empty answer options");
        }
        if (correctIndex < 0 || correctIndex > 3) {
            throw ApiException.badRequest("correctIndex must be between 0 and 3");
        }
        TriviaQuestion t = new TriviaQuestion();
        t.setQuestion(question.trim());
        t.setOptions(options.stream().map(String::trim).toList());
        t.setCorrectIndex(correctIndex);
        t.setSchool(userRepository.findById(adminId).map(User::getSchool).orElse(null));
        return TriviaRow.from(triviaQuestionRepository.save(t));
    }

    @Transactional
    public TriviaRow updateTrivia(Long id, Boolean active) {
        TriviaQuestion t = triviaQuestionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Trivia question not found"));
        if (active != null) {
            t.setActive(active);
        }
        return TriviaRow.from(triviaQuestionRepository.save(t));
    }

    @Transactional
    public void deleteTrivia(Long id) {
        triviaQuestionRepository.deleteById(id);
    }

    // ----- announcements -----

    @Transactional(readOnly = true)
    public List<Announcement> announcements() {
        return announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public Announcement createAnnouncement(String title, String body) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            throw ApiException.badRequest("Title and body are required");
        }
        Announcement a = new Announcement();
        a.setTitle(title.trim());
        a.setBody(body.trim());
        return announcementRepository.save(a);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    public record MemoryRow(Long id, Long authorId, String authorName, String authorEmail,
                            int dayNumber, String question, String answer, String mediaUrl,
                            boolean deleted, java.time.LocalDateTime createdAt) {
        static MemoryRow from(Memory m) {
            com.keepsake.backend.user.User u = m.getUser();
            return new MemoryRow(m.getId(), u.getId(), u.getFullName(), u.getEmail(),
                    m.getDayNumber(), m.getQuestion() != null ? m.getQuestion().getQuestion() : null,
                    m.getAnswer(), m.getMediaUrl(), m.isDeleted(), m.getCreatedAt());
        }
    }

    public record CommentRow(Long id, Long memoryId, Long authorId, String authorName,
                             String body, boolean deleted, java.time.LocalDateTime createdAt) {
        static CommentRow from(Comment c) {
            com.keepsake.backend.user.User u = c.getUser();
            return new CommentRow(c.getId(), c.getMemory().getId(), u.getId(), u.getFullName(),
                    c.getBody(), c.isDeleted(), c.getCreatedAt());
        }
    }

    // ----- rows -----

    public record UserRow(Long id, String email, String fullName, String nickname, String username,
                          String role, boolean active, boolean verified, String schoolName,
                          long memories, java.time.LocalDateTime createdAt) {
        static UserRow from(User u, long memories) {
            return new UserRow(u.getId(), u.getEmail(), u.getFullName(), u.getNickname(), u.getUsername(),
                    u.getRole().name(), u.isActive(), u.isVerified(),
                    u.getSchool() != null ? u.getSchool().getName() : null,
                    memories,
                    u.getCreatedAt());
        }
    }

    public record QuestionRow(Long id, int dayNumber, String question, String hint, boolean active) {
        static QuestionRow from(ChallengeQuestion q) {
            return new QuestionRow(q.getId(), q.getDayNumber(), q.getQuestion(), q.getHint(), q.isActive());
        }
    }

    public record TriviaRow(Long id, String question, List<String> options, int correctIndex,
                            boolean active, String schoolName, java.time.LocalDateTime createdAt) {
        static TriviaRow from(TriviaQuestion t) {
            return new TriviaRow(t.getId(), t.getQuestion(), t.getOptions(), t.getCorrectIndex(),
                    t.isActive(), t.getSchool() != null ? t.getSchool().getName() : null, t.getCreatedAt());
        }
    }
}
