package tw.petpick.petpick.controller;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.dto.ApplicationDTO;
import tw.petpick.petpick.repository.UserinfoRepository;
import tw.petpick.petpick.service.AdoptApplicationService;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class AdoptApplicationAdminController {

    private final AdoptApplicationService svc;
    private final UserinfoRepository userRepo;

    /** 由目前登入者(Principal.name=accountemail) 反查 DB 取得 userId */
    private Long reviewerId(Principal principal){
        return userRepo.findByAccountemail(principal.getName())
                .map(u -> u.getUserId())
                .orElseThrow(); // 沒登入或不存在會被全域的 Security 擋掉
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<ApplicationDTO> list(@RequestParam(required=false) String status,
                                     @RequestParam(required=false) String species,
                                     @RequestParam(required=false) String q,
                                     @PageableDefault(size=24) Pageable pageable) {

        // normalize: "all" / "全部" / 空字串 都當成沒選
        String st = (status == null || status.isBlank()
                || "all".equalsIgnoreCase(status) || "全部".equals(status)) ? null : status;

        String sp = (species == null || species.isBlank()
                || "all".equalsIgnoreCase(species) || "全部".equals(species)) ? null : species;

        String k  = (q == null || q.isBlank()) ? null : q;

        return svc.adminSearch(st, sp, k, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ApplicationDTO get(@PathVariable Long id){
        return svc.get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public void approve(@PathVariable Long id, Principal principal){
        svc.approve(id, reviewerId(principal));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public void reject(@PathVariable Long id,
                       @RequestParam(required=false) String reason,
                       Principal principal){
        svc.reject(id, reviewerId(principal), reason);
    }
}
