package tw.petpick.petpick.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.model.enums.SourceType;
import tw.petpick.petpick.model.enums.ApplicationStatus;
import tw.petpick.petpick.repository.AdoptApplicationRepository; 
import tw.petpick.petpick.repository.AdoptPostRepository;

import static tw.petpick.petpick.repository.AdoptPostSpecs.*;

@RestController
@RequestMapping("/api/adopts")
@RequiredArgsConstructor
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
      HttpSession session
  ) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

    boolean isAdmin = "ADMIN".equals(String.valueOf(session.getAttribute("role")));
    PostStatus stParam = parseStatus(status);
    PostStatus st = isAdmin ? stParam : PostStatus.approved;

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

    // ===== 組 pendingApplications / appliedByMe =====
    List<AdoptPost> content = pageObj.getContent();
    if (!content.isEmpty()) {
      List<Long> ids = content.stream().map(AdoptPost::getId).toList();

      // pending 數量
      Map<Long, Long> pendingMap = appRepo.countPendingByPostIds(ids).stream()
          .collect(Collectors.toMap(
              row -> ((Number) row[0]).longValue(),
              row -> ((Number) row[1]).longValue()
          ));

      // 目前登入者是否已申請（pending / approved 都算）
      Object uidObj = session.getAttribute("uid");
      Set<Long> appliedSet = Collections.emptySet();
      if (uidObj instanceof Long uid) {
        List<ApplicationStatus> stList = List.of(ApplicationStatus.pending, ApplicationStatus.approved);
        appliedSet = new HashSet<>(appRepo.findAppliedPostIds(uid, stList, ids));
      }

      // 塞到 transient 欄位
      for (AdoptPost p : content) {
        p.setPendingApplications(pendingMap.getOrDefault(p.getId(), 0L));
        p.setAppliedByMe(appliedSet.contains(p.getId()));
      }
    }

    return pageObj; // 直接回 Page，裡面的 content 已經有 transient 欄位
  }

  @GetMapping("/{id}")
public AdoptPost get(@PathVariable Long id, HttpSession session) {
    AdoptPost p = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (p.getStatus() != PostStatus.approved) {
      boolean isAdmin = "ADMIN".equals(String.valueOf(session.getAttribute("role")));
      Long uid = (Long) session.getAttribute("uid");
      boolean isOwner = uid != null && p.getPostedByUserId() != null && p.getPostedByUserId().equals(uid);
      if (!isAdmin && !isOwner) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // pending 數量
    Map<Long, Long> pendingMap = appRepo.countPendingByPostIds(List.of(id)).stream()
        .collect(Collectors.toMap(
            row -> ((Number) row[0]).longValue(),
            row -> ((Number) row[1]).longValue()
        ));
        p.setPendingApplications(pendingMap.getOrDefault(id, 0L));

        // 是否已申請 + 我的 pending 申請 id
        Object uidObj = session.getAttribute("uid");
        if (uidObj instanceof Long uid) {
          var opt = appRepo.findTopByPostIdAndApplicantUserIdOrderByIdDesc(id, uid);
          boolean applied = opt.isPresent() &&
                            (opt.get().getStatus() == ApplicationStatus.pending ||
                            opt.get().getStatus() == ApplicationStatus.approved);
          p.setAppliedByMe(applied);

          opt.filter(a -> a.getStatus() == ApplicationStatus.pending)
            .ifPresent(a -> p.setMyPendingApplicationId(a.getId()));
        } else {
          p.setAppliedByMe(false);
        }

        return p;
    }

  private PostStatus parseStatus(String s) {
    if (s == null) return null;
    s = s.trim();
    if (s.isEmpty() || s.equalsIgnoreCase("all")) return null;
    try { return PostStatus.valueOf(s.toLowerCase()); }
    catch (IllegalArgumentException e) { return null; }
  }
}
