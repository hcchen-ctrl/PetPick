package com.petpick.petpick_server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick_server.entity.Mission;
import com.petpick.petpick_server.entity.UserInfo;
import com.petpick.petpick_server.repository.MissionRepository;
import com.petpick.petpick_server.repository.UserinfoRepository;

@Service
public class ScoringService {

    private final MissionRepository missionRepo;
    private final UserinfoRepository userRepo;

    public ScoringService(MissionRepository missionRepo, UserinfoRepository userRepo) {
        this.missionRepo = missionRepo;
        this.userRepo = userRepo;
    }

    /**
     * 針對某位使用者，計算所有「可見任務」的分數，更新 Mission.score，並依分數排序回傳。
     * 可在「任務列表查詢」前先呼叫；或做成排程每日重算。
     */
    @Transactional
    public List<Mission> recomputeForUser(Long userId) {
        UserInfo u = userRepo.findById(userId).orElse(null);

        // 先抓「有效任務」：尚未結束 & 非自己發布（若需要）
        List<Mission> missions = missionRepo.findActiveMissions(LocalDateTime.now(), userId);

        // 做 price 的 min-max 正規化需要統計
        DoubleSummaryStatistics priceStats = missions.stream()
                .map(Mission::getPrice)
                .filter(p -> p != null && p > 0)
                .mapToDouble(Integer::doubleValue)
                .summaryStatistics();
        double pMin = priceStats.getCount() > 0 ? priceStats.getMin() : 0d;
        double pMax = priceStats.getCount() > 0 ? priceStats.getMax() : 1d;

        for (Mission m : missions) {
            double score = 0.0;

            // ===== 1) 地緣相容 =====
            // 同 city +0.3，同 district 再 +0.2（使用者沒資料時給 0）
            score += localityScore(u, m);

            // ===== 2) 價格正規化（越高分越高）=====
            score += priceScore(m.getPrice(), pMin, pMax) * 0.35;

            // ===== 3) 時間貼近度（距今越近越高，過期為 0）=====
            score += timeProximityScore(m.getStartTime(), m.getEndTime()) * 0.30;

            // 保底到 0~1 範圍（雙保險）
            if (score < 0)
                score = 0;
            if (score > 1)
                score = 1;

            m.setScore((int) Math.round(score * 100));
        }

        // 批次更新 score
        missionRepo.saveAll(missions);
        // 也回傳排序好的清單，方便你直接用
        missions.sort((a, b) -> Double.compare(b.getScore() != null ? b.getScore() : 0,
                a.getScore() != null ? a.getScore() : 0));
        return missions;
    }

    private double localityScore(UserInfo u, Mission m) {
        if (u == null)
            return 0.0;
        String uc = safe(u.getCity()), ud = safe(u.getDistrict());
        String mc = safe(m.getCity()), md = safe(m.getDistrict());
        double s = 0.0;
        if (!uc.isEmpty() && uc.equalsIgnoreCase(mc))
            s += 0.30;
        if (!ud.isEmpty() && ud.equalsIgnoreCase(md))
            s += 0.20;
        return s; // 0 ~ 0.5
    }

    private double priceScore(Integer price, double min, double max) {
        if (price == null || price <= 0)
            return 0.5; // 中立分
        if (max <= min)
            return 1.0; // 只有單點或統計異常時給滿分避免除以 0
        return (price - min) / (max - min); // 線性正規化到 0~1
    }

    private double timeProximityScore(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (end != null && end.isBefore(now))
            return 0.0; // 過期
        LocalDateTime ref = start != null ? start : end; // 取離現在最近的時間點
        if (ref == null)
            return 0.5;

        long days = Math.max(0, Duration.between(now, ref).toDays());
        // 0 天=1分；7天=~0.6；30天=~0.25；>60天趨近 0
        double s = 1.0 / Math.log10(10 + days); // 平滑遞減
        if (s < 0)
            s = 0;
        if (s > 1)
            s = 1;
        return s;
    }

    @Transactional
    public void resetScores() {
        missionRepo.updateAllScoresToZero();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}