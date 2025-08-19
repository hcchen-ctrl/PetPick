package tw.petpick.petpick.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.repository.PostReviewRepository;
import tw.petpick.petpick.service.AdoptPostService;

@RestController
@RequestMapping("/api/posts") // 保持現有路徑，前端不需改
public class AdoptPostAdminController {

    private final AdoptPostService service;
    private final PostReviewRepository reviewRepo;

    public AdoptPostAdminController(AdoptPostService service, PostReviewRepository reviewRepo) {
        this.service = service;
        this.reviewRepo = reviewRepo;
    }

    @PatchMapping("/{id}/status")
    public void update(@PathVariable Long id,
                       @RequestParam PostStatus status,
                       @RequestParam(required = false) String reason,
                       HttpSession session) {
        requireAdmin(session);
        Long reviewerId = (Long) session.getAttribute("uid"); 
        // TODO: 若需要，先查原狀態並做狀態機檢查
        service.updateStatusAndLog(id, status, reviewerId, reason);
    }

    @GetMapping("/{id}/reviews")
    public Object listReviews(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return reviewRepo.findByPostIdOrderByCreatedAtDesc(id);
    }

    private void requireAdmin(HttpSession s) {
        if (!"ADMIN".equals(String.valueOf(s.getAttribute("role")))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅管理員可操作");
        }
    }
}
