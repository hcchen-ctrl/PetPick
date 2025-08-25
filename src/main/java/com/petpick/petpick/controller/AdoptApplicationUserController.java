package com.petpick.petpick.controller;

import java.util.Map;

import com.petpick.petpick.DTO.ApplicationDTO;
import com.petpick.petpick.service.AdoptApplicationService;
import com.petpick.petpick.service.MyUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdoptApplicationUserController {

    private final AdoptApplicationService svc;

    // 利用 Spring Security 取得目前認證用戶的 ID
    private Long requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof MyUserDetails) {
            return ((MyUserDetails) principal).getId();
        }

        throw new RuntimeException("Cannot get user ID from principal");
    }


    // 添加這個方法來提供 CSRF token
    @GetMapping("/csrf-token")
    public Map<String, String> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            return Map.of("token", csrf.getToken());
        }
        return Map.of("error", "No CSRF token found");
    }


    // adopt-view.html 會打這支
    @PostMapping(
            value = "/adopts/{postId}/apply",
            consumes = "application/json",
            produces = "application/json")
    public ApplicationDTO apply(@PathVariable Long postId,
                                @RequestBody(required = false) Map<String,Object> in){
        Long uid = requireUser();
        String msg = in == null ? null : String.valueOf(in.getOrDefault("message", null));
        return svc.apply(postId, uid, msg);
    }

    // 我的申請列表 (my-apply.html)
    @GetMapping("/my/applications")
    public Page<ApplicationDTO> myApps(@RequestParam(required = false) String status,
                                       @PageableDefault(size = 12) Pageable pageable){
        Long uid = requireUser();
        return svc.myApps(uid, status, pageable);
    }

    // 取消申請（只有 pending）
    @PatchMapping("/applications/{id}/cancel")
    public void cancel(@PathVariable Long id){
        Long uid = requireUser();
        svc.cancel(id, uid);
    }
}
