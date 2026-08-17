package com.keepsake.backend.memory;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.challenge.ChallengeQuestion;
import com.keepsake.backend.challenge.ChallengeQuestionRepository;
import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.notification.NotificationService;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final ChallengeQuestionRepository questionRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public MemoryService(MemoryRepository memoryRepository,
                         ChallengeQuestionRepository questionRepository,
                         ReactionRepository reactionRepository,
                         CommentRepository commentRepository,
                         NotificationService notificationService,
                         UserRepository userRepository) {
        this.memoryRepository = memoryRepository;
        this.questionRepository = questionRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // ----- challenge submission -----

    @Transactional
    public MemoryDto submit(User user, int dayNumber, String answer, String mood, String mediaUrl, String mediaType) {
        user = managed(user);
        ChallengeQuestion question = questionRepository.findByDayNumber(dayNumber)
                .orElseThrow(() -> ApiException.notFound("There is no challenge question for day " + dayNumber));
        if (!question.isActive()) {
            throw ApiException.badRequest("This question is no longer active");
        }
        if (answer == null || answer.isBlank()) {
            throw ApiException.badRequest("Write something before saving your memory");
        }
        if (answer.length() > 5000) {
            throw ApiException.badRequest("Your memory is too long (max 5000 characters)");
        }
        if (memoryRepository.findByUserIdAndDayNumber(user.getId(), dayNumber).isPresent()) {
            throw ApiException.conflict("You already answered this day. You can edit your existing memory instead.");
        }

        Memory memory = new Memory();
        memory.setUser(user);
        memory.setQuestion(question);
        memory.setDayNumber(dayNumber);
        memory.setAnswer(answer.trim());
        memory.setMood(mood);
        memory.setMediaUrl(mediaUrl);
        memory.setMediaType(mediaType);
        memoryRepository.save(memory);

        notificationService.notifyAchievements(user);
        return MemoryDto.from(memory, 0, 0, false);
    }

    @Transactional
    public MemoryDto update(User user, Long memoryId, String answer, String mood, String mediaUrl, String mediaType) {
        user = managed(user);
        Memory memory = getOwned(user, memoryId);
        if (answer != null && !answer.isBlank()) {
            memory.setAnswer(answer.trim());
        }
        if (mood != null) {
            memory.setMood(mood);
        }
        if (mediaUrl != null) {
            memory.setMediaUrl(mediaUrl);
            memory.setMediaType(mediaType);
        }
        memoryRepository.save(memory);
        long likes = reactionRepository.countByMemoryId(memoryId);
        long comments = commentRepository.countByMemoryIdAndDeletedFalse(memoryId);
        boolean likedByMe = reactionRepository.findByMemoryIdAndUserId(memoryId, user.getId()).isPresent();
        return MemoryDto.from(memory, likes, comments, likedByMe);
    }

    @Transactional
    public void delete(User user, Long memoryId, boolean isAdmin) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (!isAdmin && !memory.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only delete your own memories");
        }
        memory.setDeleted(true);
        memoryRepository.save(memory);
    }

    // ----- feed / read -----

    @Transactional(readOnly = true)
    public MemoryDto get(Long memoryId, Long viewerId) {
        Memory memory = memoryRepository.findWithUserById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (memory.isDeleted()) {
            throw ApiException.notFound("Memory not found");
        }
        return MemoryDto.from(memory,
                reactionRepository.countByMemoryId(memoryId),
                commentRepository.countByMemoryIdAndDeletedFalse(memoryId),
                viewerId != null && reactionRepository.findByMemoryIdAndUserId(memoryId, viewerId).isPresent());
    }

    @Transactional(readOnly = true)
    public PageResponse<MemoryDto> feed(Long viewerId, Long userId, Long schoolId, Integer dayNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Memory> source = memoryRepository.feed(userId, schoolId, dayNumber, pageable);
        return PageResponse.of(source.map(m -> {
            long likes = reactionRepository.countByMemoryId(m.getId());
            long comments = commentRepository.countByMemoryIdAndDeletedFalse(m.getId());
            boolean likedByMe = viewerId != null
                    && reactionRepository.findByMemoryIdAndUserId(m.getId(), viewerId).isPresent();
            return MemoryDto.from(m, likes, comments, likedByMe);
        }));
    }

    /** All of a user's memories, ordered by day — used for profile + challenge progress. */
    @Transactional(readOnly = true)
    public List<MemoryDto> byUser(Long userId, Long viewerId) {
        return memoryRepository.findAllByUserIdOrderByDay(userId).stream()
                .map(m -> MemoryDto.from(m,
                        reactionRepository.countByMemoryId(m.getId()),
                        commentRepository.countByMemoryIdAndDeletedFalse(m.getId()),
                        viewerId != null && reactionRepository.findByMemoryIdAndUserId(m.getId(), viewerId).isPresent()))
                .toList();
    }

    // ----- reactions -----

    @Transactional
    public boolean toggleReaction(User user, Long memoryId, String type) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (memory.isDeleted()) {
            throw ApiException.notFound("Memory not found");
        }
        String reactionType = (type == null || type.isBlank()) ? "LIKE" : type.toUpperCase();
        var existing = reactionRepository.findByMemoryIdAndUserId(memoryId, user.getId());
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return false;
        }
        Reaction reaction = new Reaction();
        reaction.setMemory(memory);
        reaction.setUser(user);
        reaction.setType(reactionType);
        reactionRepository.save(reaction);
        notificationService.notifyLiked(memory, user);
        return true;
    }

    // ----- comments -----

    @Transactional
    public CommentDto addComment(User user, Long memoryId, String body) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (memory.isDeleted()) {
            throw ApiException.notFound("Memory not found");
        }
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("Comment cannot be empty");
        }
        if (body.length() > 2000) {
            throw ApiException.badRequest("Comment is too long (max 2000 characters)");
        }
        Comment comment = new Comment();
        comment.setMemory(memory);
        comment.setUser(user);
        comment.setBody(body.trim());
        commentRepository.save(comment);
        notificationService.notifyCommented(memory, user);
        return CommentDto.from(comment);
    }

    @Transactional
    public void deleteComment(User user, Long commentId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
        if (!isAdmin && !comment.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only delete your own comments");
        }
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentDto> comments(Long memoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(commentRepository
                .findByMemoryIdAndDeletedFalseOrderByCreatedAtAsc(memoryId, pageable)
                .map(CommentDto::from));
    }

    // ----- helpers -----

    /** Re-attaches the controller-provided (detached) user inside the transaction. */
    private User managed(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    private Memory getOwned(User user, Long memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));
        if (!memory.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only edit your own memories");
        }
        return memory;
    }
}
