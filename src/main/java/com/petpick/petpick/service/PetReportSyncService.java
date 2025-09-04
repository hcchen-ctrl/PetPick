package com.petpick.petpick.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.petpick.petpick.entity.AdoptApplication;
import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.entity.PetReportAdoption;
import com.petpick.petpick.model.enums.ApplicationStatus;
import com.petpick.petpick.repository.AdoptApplicationRepository;
import com.petpick.petpick.repository.AdoptPostRepository;
import com.petpick.petpick.repository.PetReportAdoptionRepository;
import com.petpick.petpick.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetReportSyncService {

    private final AdoptApplicationRepository appRepo;
    private final AdoptPostRepository postRepo;
    private final PetReportAdoptionRepository adoptionRepo;
    private final UserRepository userRepo;

    /** 給「單筆核准」後呼叫；若已存在就跳過（或只補圖片） */
    @Transactional
    public void syncByApplicationId(Long appId) {
        AdoptApplication a = appRepo.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "application not found"));

        // 只在「已核准」時建立追蹤名單
        if (a.getStatus() != ApplicationStatus.approved) return;

        Long postId = a.getPostId();
        Long adopterId = a.getApplicantUserId();

        // 已有名單 → 只在 image_url 為空時補上圖片就好
        Optional<PetReportAdoption> existed =
                adoptionRepo.findByPostIdExtAndAdopterUserIdExt(postId, adopterId);
        if (existed.isPresent()) {
            PetReportAdoption rec = existed.get();
            if (rec.getImageUrl() == null || rec.getImageUrl().isBlank()) {
                AdoptPost p = postRepo.findById(postId).orElse(null);
                if (p != null) {
                    rec.setImageUrl(firstImage(p));
                    adoptionRepo.save(rec);
                }
            }
            return;
        }

        // 不存在 → 建一筆
        AdoptPost post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));

        String ownerName = userRepo.findById(adopterId)
                .map(u -> u.getUsername()) // 你的 Userinfo 欄位名若不同，自行對一下
                .orElse("未命名");

        LocalDate adoptDate = (a.getApprovedAt() != null)
                ? a.getApprovedAt().toLocalDate()
                : LocalDate.now();

        PetReportAdoption rec = PetReportAdoption.builder()
                .ownerName(ownerName)
                .petName(post.getTitle())
                .adoptionDate(adoptDate)
                .imageUrl(firstImage(post))
                .requiredReports(12)
                .status("active")                 // 你的表用 varchar，值為 active
                .postIdExt(postId)
                .adopterUserIdExt(adopterId)
                .build();

        adoptionRepo.save(rec);
    }

    private String firstImage(AdoptPost p) {
        if (p == null) return null;
        if (p.getImage1() != null && !p.getImage1().isBlank()) return p.getImage1();
        if (p.getImage2() != null && !p.getImage2().isBlank()) return p.getImage2();
        if (p.getImage3() != null && !p.getImage3().isBlank()) return p.getImage3();
        return null;
    }
}
