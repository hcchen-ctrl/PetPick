package tw.petpick.petpick.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.dto.ApplicationDTO;
import tw.petpick.petpick.service.AdoptApplicationService;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class AdoptApplicationAdminController {

    private final AdoptApplicationService svc;

    private void requireAdmin(HttpSession s){
        Object role = s.getAttribute("role");
        if (role == null || !"ADMIN".equals(role.toString()))
            throw new RuntimeException("FORBIDDEN");
    }

    @GetMapping
    public Page<ApplicationDTO> list(
            @RequestParam(required=false) String status,
            @RequestParam(required=false) String species,
            @RequestParam(required=false) String q,
            @PageableDefault(size=24) Pageable pageable,
            HttpSession session){
        requireAdmin(session);
        
        // normalize: "all" / "全部" / 空字串 都當成沒選
        String st = (status == null || status.isBlank()
                || "all".equalsIgnoreCase(status) || "全部".equals(status)) ? null : status;

        String sp = (species == null || species.isBlank()
                || "all".equalsIgnoreCase(species) || "全部".equals(species)) ? null : species;

        String k  = (q == null || q.isBlank()) ? null : q;

        return svc.adminSearch(st, sp, k, pageable);
    }

    @GetMapping("/{id}")
    public ApplicationDTO get(@PathVariable Long id, HttpSession session){
        requireAdmin(session);
        return svc.get(id);
    }

    @PatchMapping("/{id}/approve")
    public void approve(@PathVariable Long id, HttpSession session){
        requireAdmin(session);
        Long reviewerId = (Long) session.getAttribute("uid");
        svc.approve(id, reviewerId);
    }

    @PatchMapping("/{id}/reject")
    public void reject(@PathVariable Long id,
                       @RequestParam(required=false) String reason,
                       HttpSession session){
        requireAdmin(session);
        Long reviewerId = (Long) session.getAttribute("uid");
        svc.reject(id, reviewerId, reason);
    }
}
