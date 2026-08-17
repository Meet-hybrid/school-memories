package com.keepsake.backend.user;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.PageResponse;
import com.keepsake.backend.memory.MemoryRepository;
import com.keepsake.backend.notification.NotificationService;
import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.ClassSetRepository;
import com.keepsake.backend.school.School;
import com.keepsake.backend.school.SchoolRepository;
import com.keepsake.backend.social.Follow;
import com.keepsake.backend.social.FollowRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;
    private final FollowRepository followRepository;
    private final MemoryRepository memoryRepository;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       SchoolRepository schoolRepository,
                       ClassSetRepository classSetRepository,
                       FollowRepository followRepository,
                       MemoryRepository memoryRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
        this.followRepository = followRepository;
        this.memoryRepository = memoryRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public UserDto profile(Long viewerId, Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserDto.from(u,
                viewerId != null && followRepository.existsByFollowerIdAndFollowingId(viewerId, userId),
                followRepository.countByFollowingId(userId),
                followRepository.countByFollowerId(userId),
                memoryRepository.countByUserIdAndDeletedFalse(userId),
                memoryRepository.countLikesReceived(userId));
    }

    /** Public profile lookup by username or id — used for profile URLs. */
    @Transactional(readOnly = true)
    public UserDto profileByHandle(Long viewerId, String handle) {
        User u;
        if (handle.matches("\\d+")) {
            u = userRepository.findById(Long.valueOf(handle))
                    .orElseThrow(() -> ApiException.notFound("User not found"));
        } else {
            u = userRepository.findByUsername(handle.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> ApiException.notFound("User not found"));
        }
        return profile(viewerId, u.getId());
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest req) {
        User u = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (req.fullName() != null && !req.fullName().isBlank()) {
            u.setFullName(req.fullName().trim());
        }
        if (req.nickname() != null) {
            u.setNickname(req.nickname().trim().isEmpty() ? null : req.nickname().trim());
        }
        if (req.bio() != null) {
            if (req.bio().length() > 500) {
                throw ApiException.badRequest("Bio is too long (max 500 characters)");
            }
            u.setBio(req.bio().trim().isEmpty() ? null : req.bio().trim());
        }
        if (req.avatarUrl() != null) {
            // empty string means "clear the avatar"
            u.setAvatarUrl(req.avatarUrl().isEmpty() ? null : req.avatarUrl());
        }
        if (req.username() != null) {
            String username = req.username().trim().toLowerCase(Locale.ROOT);
            if (username.isEmpty()) {
                throw ApiException.badRequest("Username cannot be empty");
            }
            if (!username.matches("[a-z0-9_.]{3,30}")) {
                throw ApiException.badRequest("Username may only contain lowercase letters, numbers, dots and underscores (3-30 chars)");
            }
            userRepository.findByUsername(username).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw ApiException.conflict("That username is taken");
                }
            });
            u.setUsername(username);
        }
        if (req.graduationYear() != null) {
            u.setGraduationYear(req.graduationYear());
        }
        if (req.classSetId() != null) {
            ClassSet set = classSetRepository.findById(req.classSetId())
                    .orElseThrow(() -> ApiException.badRequest("Unknown set"));
            if (u.getSchool() != null && !set.getSchool().getId().equals(u.getSchool().getId())) {
                throw ApiException.badRequest("That set does not belong to your school");
            }
            u.setClassSet(set);
        }
        userRepository.save(u);
        return profile(userId, userId);
    }

    @Transactional
    public UserDto setAvatar(Long userId, String avatarUrl) {
        User u = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setAvatarUrl(avatarUrl);
        userRepository.save(u);
        return profile(userId, userId);
    }

    // ----- follow -----

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw ApiException.badRequest("You cannot follow yourself");
        }
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return;
        }
        User follower = userRepository.findById(followerId).orElseThrow();
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);
        notificationService.notifyFollowed(following, follower);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .ifPresent(followRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> followers(Long viewerId, Long userId, int page, int size) {
        return PageResponse.of(followRepository.findByFollowingIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(f -> profile(viewerId, f.getFollower().getId())));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> following(Long viewerId, Long userId, int page, int size) {
        return PageResponse.of(followRepository.findByFollowerIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(f -> profile(viewerId, f.getFollowing().getId())));
    }

    // ----- discovery -----

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(Long viewerId, Long schoolId, String q, Long setId, Integer year, int page, int size) {
        return PageResponse.of(userRepository.search(schoolId, q, setId, year, PageRequest.of(page, size))
                .map(u -> profile(viewerId, u.getId())));
    }

    @Transactional(readOnly = true)
    public List<UserDto> suggested(Long viewerId) {
        User me = userRepository.findById(viewerId).orElseThrow();
        if (me.getSchool() == null) {
            return List.of();
        }
        return userRepository.findSuggested(me.getSchool().getId(), viewerId, PageRequest.of(0, 12)).stream()
                .map(u -> profile(viewerId, u.getId()))
                .toList();
    }

    // ----- DTO -----

    public record UpdateProfileRequest(String fullName, String nickname, String username, String bio,
                                       String avatarUrl, Long classSetId, Integer graduationYear) {
    }
}
