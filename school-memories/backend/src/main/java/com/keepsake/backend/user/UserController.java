package com.keepsake.backend.user;

import java.util.List;

import com.keepsake.backend.memory.MemoryDto;
import com.keepsake.backend.memory.MemoryService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.common.StorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final StorageService storageService;
    private final MemoryService memoryService;

    public UserController(UserService userService, StorageService storageService,
                          MemoryService memoryService) {
        this.userService = userService;
        this.storageService = storageService;
        this.memoryService = memoryService;
    }

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        return userService.profile(currentUserId(auth), currentUserId(auth));
    }

    @PatchMapping("/me")
    public UserDto updateMe(Authentication auth, @Valid @RequestBody UserService.UpdateProfileRequest req) {
        return userService.updateProfile(currentUserId(auth), req);
    }

    @PostMapping("/me/avatar")
    public UserDto uploadAvatar(Authentication auth, @RequestPart("file") MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw ApiException.badRequest("Avatar must be an image");
        }
        return userService.setAvatar(currentUserId(auth), storageService.store(file));
    }

    @GetMapping("/{handle}")
    public UserDto profile(@PathVariable String handle, Authentication auth) {
        return userService.profileByHandle(currentUserIdOrNull(auth), handle);
    }

    @GetMapping("/{id}/memories")
    public List<MemoryDto> memories(@PathVariable Long id, Authentication auth) {
        return memoryService.byUser(id, currentUserIdOrNull(auth));
    }

    // ----- follow -----

    @PostMapping("/{id}/follow")
    public UserDto follow(@PathVariable Long id, Authentication auth) {
        Long viewerId = currentUserId(auth);
        userService.follow(viewerId, id);
        return userService.profile(viewerId, id);
    }

    @PostMapping("/{id}/unfollow")
    public UserDto unfollow(@PathVariable Long id, Authentication auth) {
        Long viewerId = currentUserId(auth);
        userService.unfollow(viewerId, id);
        return userService.profile(viewerId, id);
    }

    @GetMapping("/{id}/followers")
    public PageResponse<UserDto> followers(@PathVariable Long id, Authentication auth,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return userService.followers(currentUserIdOrNull(auth), id, page, size);
    }

    @GetMapping("/{id}/following")
    public PageResponse<UserDto> following(@PathVariable Long id, Authentication auth,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return userService.following(currentUserIdOrNull(auth), id, page, size);
    }

    // ----- discovery -----

    @GetMapping("/search")
    public PageResponse<UserDto> search(Authentication auth,
                                        @RequestParam(required = false) Long schoolId,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Long set,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return userService.search(currentUserId(auth), schoolId, q, set, year, page, size);
    }

    @GetMapping("/suggested")
    public List<UserDto> suggested(Authentication auth) {
        return userService.suggested(currentUserId(auth));
    }

    private Long currentUserId(Authentication auth) {
        return Long.valueOf(((UserDetails) auth.getPrincipal()).getUsername());
    }

    private Long currentUserIdOrNull(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails details)) {
            return null;
        }
        return Long.valueOf(details.getUsername());
    }
}
