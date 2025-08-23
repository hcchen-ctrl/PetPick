package com.petpick.petpick.controller;

import java.util.List;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import com.petpick.petpick.repository.AdoptPostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;


@RestController
@RequestMapping("/api/posts")
public class AdoptPostController {

    private final AdoptPostRepository postRepo;

    public AdoptPostController(AdoptPostRepository postRepo) {
        this.postRepo = postRepo;
    }

    /** 建立刊登
     *  - ADMIN：我方救助 + 直接上架 (approved)
     *  - 會員  ：民眾送養 + 等待審核 (pending)
     */
    @PostMapping
    public AdoptPost create(@RequestBody AdoptPost in, HttpSession session) {
        // ✅ 這裡加上 log
        System.out.println("Create called: " + in.getTitle());
        long uid = getUid(session);
        String role = String.valueOf(session.getAttribute("role"));

        if ("ADMIN".equals(role)) {
            in.setSourceType(SourceType.platform);
            in.setPostedByEmployeeId(uid);
            in.setStatus(PostStatus.approved);     // ★ 直接通過上架
        } else {
            in.setSourceType(SourceType.user);
            in.setPostedByUserId(uid);
            in.setStatus(PostStatus.pending);      // ★ 進入審核
        }
        return postRepo.save(in);
    }

    /** 讀自己的刊登（會員用；ADMIN 不允許） */
    @GetMapping("/my")
    public List<AdoptPost> myPosts(@RequestParam(required = false) PostStatus status,
                                   HttpSession session) {
        Object uidObj = session.getAttribute("uid");
        if (uidObj == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理員請使用審核中心");

        long uid = ((Number) uidObj).longValue();
        return (status == null)
                ? postRepo.findByPostedByUserIdOrderByCreatedAtDesc(uid)
                : postRepo.findByPostedByUserIdAndStatusOrderByCreatedAtDesc(uid, status);
    }

    /** 取消刊登（擁有者或管理員）→ cancelled */
    @PatchMapping("/{id}/cancel")
    public AdoptPost cancel(@PathVariable Long id, HttpSession session) {
        long uid = getUid(session);
        String role = String.valueOf(session.getAttribute("role"));
        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");
        if (p.getStatus()==PostStatus.closed || p.getStatus()==PostStatus.rejected || p.getStatus()==PostStatus.cancelled)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此狀態不可取消");

        p.setStatus(PostStatus.cancelled);
        return postRepo.save(p);
    }

    /** 下架（已送養完成）→ closed */
    @PatchMapping("/{id}/close")
    public AdoptPost close(@PathVariable Long id, HttpSession session) {
        long uid = getUid(session);
        String role = String.valueOf(session.getAttribute("role"));
        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");
        if (p.getStatus()!=PostStatus.approved && p.getStatus()!=PostStatus.on_hold)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已上架/暫停中可下架");

        p.setStatus(PostStatus.closed);
        return postRepo.save(p);
    }

    /** 暫停/恢復 */
    @PatchMapping("/{id}/hold")
    public AdoptPost hold(@PathVariable Long id,
                          @RequestParam(defaultValue = "true") boolean hold,
                          HttpSession session) {
        long uid = getUid(session);
        String role = String.valueOf(session.getAttribute("role"));
        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");

        if (hold) {
            if (p.getStatus()!=PostStatus.approved)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已上架可暫停");
            p.setStatus(PostStatus.on_hold);
        } else {
            if (p.getStatus()!=PostStatus.on_hold)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有暫停中可恢復");
            p.setStatus(PostStatus.approved);
        }
        return postRepo.save(p);
    }

    // ========= helpers =========
    private long getUid(HttpSession s) {
        Object uid = s.getAttribute("uid");
        if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        return ((Number) uid).longValue();
    }
    private void requireAdmin(HttpSession s) {
        if (!"ADMIN".equals(String.valueOf(s.getAttribute("role"))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅管理員可操作");
    }
    private boolean isOwnerOrAdmin(AdoptPost p, long uid, String role) {
        return "ADMIN".equals(role) ||
               (p.getPostedByUserId() != null && p.getPostedByUserId().equals(uid));
    }
}
