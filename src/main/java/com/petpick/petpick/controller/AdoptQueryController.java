package com.petpick.petpick.controller;

import com.petpick.petpick.entity.AdoptApplication;
import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.ApplicationStatus;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import com.petpick.petpick.repository.AdoptApplicationRepository;
import com.petpick.petpick.repository.AdoptPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

import static com.petpick.petpick.model.AdoptPostSpecs.*;

@CrossOrigin(origins = "http://localhost:5173") // 依你的 Vue dev server port
@RestController
@RequestMapping("/api/adopts")
@RequiredArgsConstructor
@Slf4j
public class AdoptQueryController {

    private final AdoptPostRepository repo;
    private final AdoptApplicationRepository appRepo;

    @GetMapping
    public Page<AdoptPost> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String sex,
            @RequestParam(required = false) String age,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) String status,
            Authentication authentication  // ✅ 保留但要處理 null 的情況
    ) {
        try {
            log.info("API called with params: page={}, size={}, city={}, status={}",
                    page, size, city, status);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

            // ✅ 安全地檢查認證狀態
            boolean isAdmin = authentication != null && hasRole(authentication, "ADMIN");
            Long currentUserId = authentication != null ? getCurrentUserId(authentication) : null;

            log.info("User info: isAuthenticated={}, isAdmin={}, userId={}",
                    authentication != null, isAdmin, currentUserId);

            // ✅ 對於未登入用戶，只顯示 approved 狀態的貼文
            PostStatus stParam = parseStatus(status);
            PostStatus st;
            if (authentication == null) {
                // 未登入用戶只能看 approved
                st = PostStatus.approved;
            } else if (isAdmin) {
                st = stParam; // 管理員可看全部或指定狀態
            } else {
                st = (stParam == null) ? PostStatus.approved : stParam;
            }

            log.info("Using status filter: {}", st);

            Specification<AdoptPost> spec = Specification
                    .where(statusEq(st))
                    .and(sourceType(sourceType))
                    .and(city(city))
                    .and(district(district))
                    .and(sex(sex))
                    .and(species(species))
                    .and(age(age))
                    .and(keyword(q));

            Page<AdoptPost> pageObj = repo.findAll(spec, pageable);
            log.info("Found {} posts", pageObj.getTotalElements());

            List<AdoptPost> content = pageObj.getContent();
            if (!content.isEmpty()) {
                List<Long> ids = content.stream().map(AdoptPost::getId).toList();

                // pending 數量
                Map<Long, Long> pendingMap = Collections.emptyMap();
                try {
                    pendingMap = appRepo.countPendingByPostIds(ids, ApplicationStatus.pending)
                            .stream()
                            .collect(Collectors.toMap(
                                    row -> ((Number) row[0]).longValue(),
                                    row -> ((Number) row[1]).longValue()
                            ));
                } catch (Exception e) {
                    log.warn("Failed to count pending applications", e);
                }

                // 當前用戶已申請的貼文
                Set<Long> appliedSet = Collections.emptySet();
                if (currentUserId != null) {
                    try {
                        List<ApplicationStatus> stList = List.of(ApplicationStatus.pending, ApplicationStatus.approved);
                        appliedSet = new HashSet<>(appRepo.findAppliedPostIds(currentUserId, stList, ids));
                    } catch (Exception e) {
                        log.warn("Failed to get applied posts for user {}", currentUserId, e);
                    }
                }

                for (AdoptPost p : content) {
                    p.setPendingApplications(pendingMap.getOrDefault(p.getId(), 0L));
                    p.setAppliedByMe(appliedSet.contains(p.getId()));
                }
            }

            return pageObj;

        } catch (Exception e) {
            log.error("Error in list API", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error: " + e.getMessage());
        }
    }


    // ✅ 添加缺少的方法們
    private PostStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        String normalizedStatus = status.trim().toLowerCase();
        if ("all".equals(normalizedStatus)) {
            return null;
        }

        try {
            return PostStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status parameter: {}", status);
            return null;
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        try {
            // 假設你的 JWT subject 是用戶 ID
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse user ID from authentication: {}", auth.getName());
            return null;
        }
    }

        /**
         * ✅ 新增：根據 ID 取得單一貼文詳細資料
         * 公開貼文任何人都可以看，非公開貼文需要權限
         */
        @GetMapping("/{id}")
        public AdoptPost getById(@PathVariable Long id, Authentication authentication) {
            try {
                log.info("Getting post by ID: {}", id);

                AdoptPost post = repo.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此貼文"));

                log.info("Found post: title={}, status={}", post.getTitle(), post.getStatus());

                // ✅ 如果是公開貼文，任何人都可以看
                if (post.getStatus() == PostStatus.approved) {
                    log.info("Post is approved, allowing access");

                    // 為已登入用戶添加額外資訊
                    if (authentication != null) {
                        Long currentUserId = getCurrentUserId(authentication);
                        if (currentUserId != null) {
                            // 檢查是否已申請
                            List<ApplicationStatus> statusList = List.of(ApplicationStatus.pending, ApplicationStatus.approved);
                            List<Long> appliedIds = appRepo.findAppliedPostIds(currentUserId, statusList, List.of(id));
                            post.setAppliedByMe(!appliedIds.isEmpty());

                            // 如果有待審核的申請，記錄申請 ID
                            // 這裡需要實作取得申請 ID 的邏輯
                        }
                    }

                    return post;
                }

                // ✅ 非公開貼文需要認證和權限檢查
                if (authentication == null) {
                    log.warn("Post is not approved and user is not authenticated");
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此貼文尚未公開");
                }

                boolean isAdmin = hasRole(authentication, "ADMIN");
                Long currentUserId = getCurrentUserId(authentication);

                log.info("User auth check: isAdmin={}, userId={}, postOwner={}",
                        isAdmin, currentUserId, post.getPostedByUserId());

                // 管理員可以看所有貼文，一般使用者只能看自己的貼文
                if (!isAdmin) {
                    if (post.getPostedByUserId() == null || !post.getPostedByUserId().equals(currentUserId)) {
                        log.warn("User {} has no permission to view post {}", currentUserId, id);
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限查看此貼文");
                    }
                }

                return post;

            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error getting post by ID: {}", id, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error: " + e.getMessage());
            }
        }

    // 在 AdoptQueryController 中添加申請端點
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyForAdoption(@PathVariable Long id,
                                              @RequestBody(required = false) Map<String, String> request,
                                              Authentication authentication) {
        try {
            log.info("Application request for post ID: {}", id);

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要登入才能申請");
            }

            Long currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "無法識別用戶");
            }

            // 檢查貼文是否存在且為 approved 狀態
            AdoptPost post = repo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此貼文"));

            if (post.getStatus() != PostStatus.approved) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此貼文尚未開放申請");
            }

            // 檢查是否已經申請過
            List<ApplicationStatus> existingStatuses = List.of(ApplicationStatus.pending, ApplicationStatus.approved);
            List<Long> appliedIds = appRepo.findAppliedPostIds(currentUserId, existingStatuses, List.of(id));

            if (!appliedIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "您已申請過此貼文");
            }

//             創建申請記錄 (這裡需要你的 AdoptApplication 實體)
             AdoptApplication application = new AdoptApplication();
            application.setPostId(post.getId());  // ✅ 因為你存的是 postId
            application.setApplicantUserId(currentUserId);
             application.setMessage(request != null ? request.get("message") : null);
             application.setStatus(ApplicationStatus.pending);
             appRepo.save(application);

            log.info("Application created successfully for user {} and post {}", currentUserId, id);

            return ResponseEntity.ok(Map.of("message", "申請已送出"));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating application for post {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "系統錯誤: " + e.getMessage());
        }
    }    }