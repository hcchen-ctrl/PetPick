package tw.petpick.petpick.controller;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.enums.*;
import tw.petpick.petpick.repository.AdoptApplicationRepository;
import tw.petpick.petpick.repository.AdoptPostRepository;
import tw.petpick.petpick.repository.UserinfoRepository;

import static tw.petpick.petpick.repository.AdoptPostSpecs.*;

@RestController
@RequestMapping("/api/adopts")
@RequiredArgsConstructor
public class AdoptQueryController {

  private final AdoptPostRepository repo;
  private final AdoptApplicationRepository appRepo;
  private final UserinfoRepository userRepo;

  private Long currentUidOrNull(Principal principal){
    if (principal == null) return null;
    return userRepo.findByAccountemail(principal.getName())
            .map(u -> u.getUserId())
            .orElse(null);
  }

  private boolean isAdmin(HttpServletRequest req){ return req.isUserInRole("ADMIN"); }

  @GetMapping
  public Page<AdoptPost> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              @RequestParam(required = false) String city,
                              @RequestParam(required = false) String district,
                              @RequestParam(required = false) String species,
                              @RequestParam(required = false) String sex,
                              @RequestParam(required = false) String age,
                              @RequestParam(required = false, name = "q") String q,
                              @RequestParam(required = false) SourceType sourceType,
                              @RequestParam(required = false) String status,
                              Principal principal,
                              HttpServletRequest req) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

    boolean admin = isAdmin(req);
    PostStatus stParam = parseStatus(status);
    PostStatus st = admin ? stParam : PostStatus.approved;

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

    // ===== pendingApplications / appliedByMe =====
    List<AdoptPost> content = pageObj.getContent();
    if (!content.isEmpty()) {
      List<Long> ids = content.stream().map(AdoptPost::getId).toList();

      Map<Long, Long> pendingMap = appRepo.countPendingByPostIds(ids).stream()
          .collect(Collectors.toMap(
              row -> ((Number) row[0]).longValue(),
              row -> ((Number) row[1]).longValue()
          ));

      Long uid = currentUidOrNull(principal);
      Set<Long> appliedSet = Collections.emptySet();
      if (uid != null) {
        List<ApplicationStatus> stList = List.of(ApplicationStatus.pending, ApplicationStatus.approved);
        appliedSet = new HashSet<>(appRepo.findAppliedPostIds(uid, stList, ids));
      }

      for (AdoptPost p : content) {
        p.setPendingApplications(pendingMap.getOrDefault(p.getId(), 0L));
        p.setAppliedByMe(appliedSet.contains(p.getId()));
      }
    }
    return pageObj;
  }

  @GetMapping("/{id}")
  public AdoptPost get(@PathVariable Long id,
                       Principal principal,
                       HttpServletRequest req) {
    AdoptPost p = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (p.getStatus() != PostStatus.approved) {
      boolean admin = isAdmin(req);
      Long uid = currentUidOrNull(principal);
      boolean isOwner = uid != null && p.getPostedByUserId() != null && p.getPostedByUserId().equals(uid);
      if (!admin && !isOwner) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    Map<Long, Long> pendingMap = appRepo.countPendingByPostIds(List.of(id)).stream()
        .collect(Collectors.toMap(
            row -> ((Number) row[0]).longValue(),
            row -> ((Number) row[1]).longValue()
        ));
    p.setPendingApplications(pendingMap.getOrDefault(id, 0L));

    Long uid = currentUidOrNull(principal);
    if (uid != null) {
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
