package tw.petpick.petpick.controller;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.dto.ApplicationDTO;
import tw.petpick.petpick.repository.UserinfoRepository;
import tw.petpick.petpick.service.AdoptApplicationService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdoptApplicationUserController {

    private final AdoptApplicationService svc;
    private final UserinfoRepository userRepo;

    private Long currentUid(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userRepo.findByAccountemail(principal.getName())
                .map(u -> u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // adopt-view.html 會打這支
    @PostMapping(value = "/adopts/{postId}/apply", consumes = "application/json", produces = "application/json")
    public ApplicationDTO apply(@PathVariable Long postId,
                                @RequestBody(required = false) java.util.Map<String,Object> in,
                                Principal principal){
        Long uid = currentUid(principal);
        String msg = in == null ? null : String.valueOf(in.getOrDefault("message", null));
        return svc.apply(postId, uid, msg);
    }

    // 我的申請列表 (my-apply.html)
    @GetMapping("/my/applications")
    public Page<ApplicationDTO> myApps(@RequestParam(required = false) String status,
                                       @PageableDefault(size = 12) Pageable pageable,
                                       Principal principal){
        Long uid = currentUid(principal);
        return svc.myApps(uid, status, pageable);
    }

    // 取消申請（只有 pending）
    @PatchMapping("/applications/{id}/cancel")
    public void cancel(@PathVariable Long id, Principal principal){
        Long uid = currentUid(principal);
        svc.cancel(id, uid);
    }
}
