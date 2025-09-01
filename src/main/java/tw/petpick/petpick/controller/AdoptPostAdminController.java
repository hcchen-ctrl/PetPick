package tw.petpick.petpick.controller;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.repository.PostReviewRepository;
import tw.petpick.petpick.repository.UserinfoRepository;
import tw.petpick.petpick.service.AdoptPostService;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ← 這個類別所有方法都要 ADMIN
public class AdoptPostAdminController {

    private final AdoptPostService service;
    private final PostReviewRepository reviewRepo;
    private final UserinfoRepository userRepo;

    private Long reviewerId(Principal principal){
        return userRepo.findByAccountemail(principal.getName())
                .map(u -> u.getUserId())
                .orElseThrow();
    }

    @PatchMapping("/{id}/status")
    public void update(@PathVariable Long id,
                       @RequestParam PostStatus status,
                       @RequestParam(required = false) String reason,
                       Principal principal) {
        Long rid = reviewerId(principal);
        service.updateStatusAndLog(id, status, rid, reason);
    }

    @GetMapping("/{id}/reviews")
    public Object listReviews(@PathVariable Long id) {
        return reviewRepo.findByPostIdOrderByCreatedAtDesc(id);
    }
}
