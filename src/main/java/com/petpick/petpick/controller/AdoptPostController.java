package com.petpick.petpick.controller;

import java.util.List;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import com.petpick.petpick.repository.AdoptPostRepository;
import com.petpick.petpick.service.MyUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    public AdoptPost create(@RequestBody AdoptPost in, Authentication authentication) {
        System.out.println("Create called: " + in.getTitle());

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

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
                                   Authentication authentication) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

        if ("ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理員請使用審核中心");
        }

        return (status == null)
                ? postRepo.findByPostedByUserIdOrderByCreatedAtDesc(uid)
                : postRepo.findByPostedByUserIdAndStatusOrderByCreatedAtDesc(uid, status);
    }

    /**
     * 根據 ID 取得單一貼文詳細資料
     * 管理員可以看所有貼文，一般使用者只能看自己的貼文
     */
    @GetMapping("/{id}")
    public AdoptPost getPostById(@PathVariable Long id, Authentication authentication) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

        AdoptPost post = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此貼文"));

        // 權限檢查：管理員可以看所有貼文，一般使用者只能看自己的貼文
        if (!"ADMIN".equals(role)) {
            if (post.getPostedByUserId() == null || !post.getPostedByUserId().equals(uid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限查看此貼文");
            }
        }

        return post;
    }

    /**
     * 管理員審核：通過/退回貼文
     */
    @PatchMapping("/{id}/status")
    public AdoptPost updateStatus(@PathVariable Long id,
                                  @RequestParam PostStatus status,
                                  @RequestParam(required = false, defaultValue = "") String reason,
                                  Authentication authentication) {

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String role = userDetails.getRole();

        // 只有管理員可以審核
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }

        AdoptPost post = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此貼文"));

        // 只有 pending 狀態的貼文可以審核
        if (post.getStatus() != PostStatus.pending) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待審核的貼文可以進行審核");
        }

        post.setStatus(status);

        // 如果是退回且有原因，可以記錄退回原因（這裡假設你有對應的欄位）
        if (status == PostStatus.rejected && !reason.isEmpty()) {
            // post.setRejectReason(reason); // 如果你的實體有這個欄位的話
        }

        return postRepo.save(post);
    }


    /** 取消刊登（擁有者或管理員）→ cancelled */
    @PatchMapping("/{id}/cancel")
    public AdoptPost cancel(@PathVariable Long id, Authentication authentication) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");
        }
        if (p.getStatus()==PostStatus.closed || p.getStatus()==PostStatus.rejected || p.getStatus()==PostStatus.cancelled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此狀態不可取消");
        }

        p.setStatus(PostStatus.cancelled);
        return postRepo.save(p);
    }

    /** 下架（已送養完成）→ closed */
    @PatchMapping("/{id}/close")
    public AdoptPost close(@PathVariable Long id, Authentication authentication) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");
        }
        if (p.getStatus()!=PostStatus.approved && p.getStatus()!=PostStatus.on_hold) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已上架/暫停中可下架");
        }

        p.setStatus(PostStatus.closed);
        return postRepo.save(p);
    }

    /** 暫停/恢復 */
    @PatchMapping("/{id}/hold")
    public AdoptPost hold(@PathVariable Long id,
                          @RequestParam(defaultValue = "true") boolean hold,
                          Authentication authentication) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        long uid = userDetails.getId();
        String role = userDetails.getRole();

        AdoptPost p = postRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isOwnerOrAdmin(p, uid, role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");
        }

        if (hold) {
            if (p.getStatus()!=PostStatus.approved) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已上架可暫停");
            }
            p.setStatus(PostStatus.on_hold);
        } else {
            if (p.getStatus()!=PostStatus.on_hold) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有暫停中可恢復");
            }
            p.setStatus(PostStatus.approved);
        }
        return postRepo.save(p);
    }

    // ========= helpers =========
    private boolean isOwnerOrAdmin(AdoptPost p, long uid, String role) {
        return "ADMIN".equals(role) ||
                (p.getPostedByUserId() != null && p.getPostedByUserId().equals(uid));
    }


    /**
     * 管理員審核中心 - 取得所有貼文（支援篩選和分頁）
     * 只有管理員可以存取
     */
    @GetMapping
    public Page<AdoptPost> getAllPosts(@RequestParam(required = false) PostStatus status,
                                       @RequestParam(required = false) String species,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "24") int size,
                                       Authentication authentication) {

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String role = userDetails.getRole();

        // 只有管理員可以存取審核中心
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 根據篩選條件查詢
        if (status != null && species != null && !species.isEmpty() && q != null && !q.isEmpty()) {
            // 三個條件都有
            return postRepo.findByStatusAndSpeciesContainingIgnoreCaseAndTitleContainingIgnoreCase(
                    status, species, q, pageable);
        } else if (status != null && species != null && !species.isEmpty()) {
            // 狀態 + 物種
            return postRepo.findByStatusAndSpeciesContainingIgnoreCase(status, species, pageable);
        } else if (status != null && q != null && !q.isEmpty()) {
            // 狀態 + 關鍵字
            return postRepo.findByStatusAndTitleContainingIgnoreCase(status, q, pageable);
        } else if (species != null && !species.isEmpty() && q != null && !q.isEmpty()) {
            // 物種 + 關鍵字
            return postRepo.findBySpeciesContainingIgnoreCaseAndTitleContainingIgnoreCase(
                    species, q, pageable);
        } else if (status != null) {
            // 只有狀態
            return postRepo.findByStatus(status, pageable);
        } else if (species != null && !species.isEmpty()) {
            // 只有物種
            return postRepo.findBySpeciesContainingIgnoreCase(species, pageable);
        } else if (q != null && !q.isEmpty()) {
            // 只有關鍵字
            return postRepo.findByTitleContainingIgnoreCase(q, pageable);
        } else {
            // 無篩選條件，回傳所有
            return postRepo.findAll(pageable);
        }
    }


}
