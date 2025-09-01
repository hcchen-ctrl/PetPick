package tw.petpick.petpick.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.model.enums.SourceType;
import tw.petpick.petpick.repository.AdoptPostRepository;
import tw.petpick.petpick.repository.UserinfoRepository;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class AdoptPostController {

  private final AdoptPostRepository postRepo;
  private final UserinfoRepository userRepo;

  private long currentUid(Principal principal){
    return userRepo.findByAccountemail(principal.getName())
            .map(u -> u.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private boolean isAdmin(HttpServletRequest req){ return req.isUserInRole("ADMIN"); }

  private boolean isBlank(String s){ return s == null || s.isBlank(); }

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public AdoptPost create(@RequestBody AdoptPost in,
                          Principal principal,
                          HttpServletRequest req) {
    long uid = currentUid(principal);
    boolean admin = isAdmin(req);

    in.setPostedByUserId(uid);

    // 自動帶預設聯絡資料
    var me = userRepo.findById(uid).orElse(null);
    if (me != null) {
      if (isBlank(in.getContactName()))  in.setContactName(me.getUsername());
      if (isBlank(in.getContactPhone())) in.setContactPhone(me.getPhonenumber());
    }

    if (admin) {
      in.setSourceType(SourceType.platform);
      in.setStatus(PostStatus.approved);
    } else {
      in.setSourceType(SourceType.user);
      in.setStatus(PostStatus.pending);
    }
    return postRepo.save(in);
  }

  /** 讀自己的刊登（會員用；ADMIN 不允許） */
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/my")
  public List<AdoptPost> myPosts(@RequestParam(required = false) PostStatus status,
                                 Principal principal,
                                 HttpServletRequest req) {
    if (isAdmin(req)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理員請使用審核中心");
    long uid = currentUid(principal);
    return (status == null)
        ? postRepo.findByPostedByUserIdOrderByCreatedAtDesc(uid)
        : postRepo.findByPostedByUserIdAndStatusOrderByCreatedAtDesc(uid, status);
  }

  /** 取消刊登（擁有者或管理員）→ cancelled */
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/cancel")
  public AdoptPost cancel(@PathVariable Long id,
                          Principal principal,
                          HttpServletRequest req) {
    long uid = currentUid(principal);
    boolean admin = isAdmin(req);
    AdoptPost p = postRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!admin && !(p.getPostedByUserId()!=null && p.getPostedByUserId().equals(uid)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");

    if (p.getStatus()==PostStatus.closed || p.getStatus()==PostStatus.rejected || p.getStatus()==PostStatus.cancelled)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此狀態不可取消");

    p.setStatus(PostStatus.cancelled);
    return postRepo.save(p);
  }

  /** 下架（已送養完成）→ closed */
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/close")
  public AdoptPost close(@PathVariable Long id,
                         Principal principal,
                         HttpServletRequest req) {
    long uid = currentUid(principal);
    boolean admin = isAdmin(req);
    AdoptPost p = postRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!admin && !(p.getPostedByUserId()!=null && p.getPostedByUserId().equals(uid)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");

    if (p.getStatus()!=PostStatus.approved && p.getStatus()!=PostStatus.on_hold)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已上架/暫停中可下架");

    p.setStatus(PostStatus.closed);
    return postRepo.save(p);
  }

  /** 暫停/恢復 */
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/hold")
  public AdoptPost hold(@PathVariable Long id,
                        @RequestParam(defaultValue = "true") boolean hold,
                        Principal principal,
                        HttpServletRequest req) {
    long uid = currentUid(principal);
    boolean admin = isAdmin(req);
    AdoptPost p = postRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!admin && !(p.getPostedByUserId()!=null && p.getPostedByUserId().equals(uid)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限");

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
}
