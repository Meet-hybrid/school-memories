package com.keepsake.backend.memory;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.MediaProcessor;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;
    private final UserRepository userRepository;
    private final MediaProcessor mediaProcessor;

    public MemoryController(MemoryService memoryService, UserRepository userRepository,
                            MediaProcessor mediaProcessor) {
        this.memoryService = memoryService;
        this.userRepository = userRepository;
        this.mediaProcessor = mediaProcessor;
    }

    @GetMapping
    public PageResponse<MemoryDto> feed(Authentication auth,
                                        @RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Long schoolId,
                                        @RequestParam(required = false) Integer day,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return memoryService.feed(currentUserId(auth), userId, schoolId, day, page, Math.min(size, 50));
    }

    @GetMapping("/{id}")
    public MemoryDto get(@PathVariable Long id, Authentication auth) {
        return memoryService.get(id, currentUserId(auth));
    }

    /**
     * Submit a memory for a challenge day. Multipart so an optional photo/video
     * can ride along with the text.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MemoryDto create(Authentication auth,
                            @RequestParam("day") Integer day,
                            @RequestParam("answer") @NotBlank @Size(max = 5000) String answer,
                            @RequestParam(value = "mood", required = false) String mood,
                            @RequestPart(value = "file", required = false) MultipartFile file) {
        User user = currentUser(auth);
        MediaProcessor.ProcessedMedia media = mediaProcessor.process(file);
        return memoryService.submit(user, day, answer, mood,
                media != null ? media.url() : null,
                media != null ? media.type() : null,
                media != null ? media.thumbnailUrl() : null);
    }

    @PatchMapping("/{id}")
    public MemoryDto update(@PathVariable Long id, Authentication auth,
                            @Valid @RequestBody UpdateMemoryRequest req) {
        User user = currentUser(auth);
        return memoryService.update(user, id, req.answer(), req.mood(), req.mediaUrl(), req.mediaType());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        memoryService.delete(currentUser(auth), id, isAdmin(auth));
    }

    // ----- reactions -----

    @PostMapping("/{id}/reactions")
    public Map<String, Object> react(@PathVariable Long id, Authentication auth, @RequestBody(required = false) ReactRequest req) {
        String type = req != null ? req.type() : "LIKE";
        boolean liked = memoryService.toggleReaction(currentUser(auth), id, type);
        return Map.of("liked", liked, "type", type);
    }

    // ----- comments -----

    @GetMapping("/{id}/comments")
    public PageResponse<CommentDto> comments(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return memoryService.comments(id, page, Math.min(size, 100));
    }

    @PostMapping("/{id}/comments")
    public CommentDto addComment(@PathVariable Long id, Authentication auth,
                                 @Valid @RequestBody CommentRequest req) {
        return memoryService.addComment(currentUser(auth), id, req.body());
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public void deleteComment(@PathVariable Long id, @PathVariable Long commentId, Authentication auth) {
        memoryService.deleteComment(currentUser(auth), commentId, isAdmin(auth));
    }

    // ----- helpers -----

    private User currentUser(Authentication auth) {
        UserDetails details = (UserDetails) auth.getPrincipal();
        return userRepository.findById(Long.valueOf(details.getUsername()))
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    private Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails details)) {
            return null;
        }
        return Long.valueOf(details.getUsername());
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ----- request DTOs -----

    public record UpdateMemoryRequest(
            @Size(max = 5000) String answer,
            String mood,
            String mediaUrl,
            String mediaType) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String body) {
    }

    public record ReactRequest(String type) {
    }
}
