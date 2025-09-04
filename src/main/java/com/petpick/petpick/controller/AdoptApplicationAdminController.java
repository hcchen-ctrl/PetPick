package com.petpick.petpick.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.DTO.ApplicationDTO;
import com.petpick.petpick.service.AdoptApplicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class AdoptApplicationAdminController {

    private final AdoptApplicationService svc;
    private final com.petpick.petpick.service.PetReportSyncService reportSync;

    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    @GetMapping
    public Page<ApplicationDTO> list(
            @RequestParam(required=false) String status,
            @RequestParam(required=false) String species,
            @RequestParam(required=false) String q,
            @PageableDefault(size=24) Pageable pageable) {
        requireAdmin();

        // normalize: "all" / "全部" / 空字串 都當成沒選
        String st = (status == null || status.isBlank()
                || "all".equalsIgnoreCase(status) || "全部".equals(status)) ? null : status;

        String sp = (species == null || species.isBlank()
                || "all".equalsIgnoreCase(species) || "全部".equals(species)) ? null : species;

        String k  = (q == null || q.isBlank()) ? null : q;

        return svc.adminSearch(st, sp, k, pageable);
    }

    @GetMapping("/{id}")
    public ApplicationDTO get(@PathVariable Long id) {
        requireAdmin();
        return svc.get(id);
    }

    @PatchMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        requireAdmin();
        Long reviewerId = getCurrentUserId();
        svc.approve(id, reviewerId);

        // ★ 只補這一行：核准後同步回報名單（若已存在會自動跳過）
        try { reportSync.syncByApplicationId(id); } catch (Exception ignore) {}
    }

    @PatchMapping("/{id}/reject")
    public void reject(@PathVariable Long id,
                       @RequestParam(required=false) String reason) {
        requireAdmin();
        Long reviewerId = getCurrentUserId();
        svc.reject(id, reviewerId, reason);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
        }
        // 假設你 principal 是你自己定義的 UserDetails，裡面有 getId() 方法
        Object principal = auth.getPrincipal();
        if (principal instanceof com.petpick.petpick.service.MyUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
    }
}

