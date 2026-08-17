package com.keepsake.backend.announcement;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /** Active announcements shown to everyone (authenticated route). */
    @GetMapping
    public List<Announcement> list() {
        return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
    }
}
