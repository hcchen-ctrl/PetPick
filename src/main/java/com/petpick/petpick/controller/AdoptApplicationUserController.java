package com.petpick.petpick.controller;

import java.util.Map;

import com.petpick.petpick.DTO.ApplicationDTO;
import com.petpick.petpick.service.AdoptApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    private Long requireUser(HttpSession s){
        Object uid = s.getAttribute("uid");
        if (uid == null) throw new RuntimeException("UNAUTHORIZED");
        return (Long) uid;
    }

    // adopt-view.html 會打這支
    @PostMapping(
      value = "/adopts/{postId}/apply",
      consumes = "application/json",
      produces = "application/json")
    public ApplicationDTO apply(@PathVariable Long postId,
                                @RequestBody(required=false) Map<String,Object> in,
                                HttpSession session){
        Long uid = requireUser(session);
        String msg = in == null ? null : String.valueOf(in.getOrDefault("message", null));
        return svc.apply(postId, uid, msg);
    }

    // 我的申請列表 (my-apply.html)
    @GetMapping("/my/applications")
    public Page<ApplicationDTO> myApps(@RequestParam(required=false) String status,
                                       @PageableDefault(size=12) Pageable pageable,
                                       HttpSession session){
        Long uid = requireUser(session);
        return svc.myApps(uid, status, pageable);
    }

    // 取消申請（只有 pending）
    @PatchMapping("/applications/{id}/cancel")
    public void cancel(@PathVariable Long id, HttpSession session){
        Long uid = requireUser(session);
        svc.cancel(id, uid);
    }
}
