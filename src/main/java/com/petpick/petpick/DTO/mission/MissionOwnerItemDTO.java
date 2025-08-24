package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;
import java.util.List;

public record MissionOwnerItemDTO(
  Long missionId, String title, String city, String district,
  LocalDateTime startTime, LocalDateTime endTime, int price,
  List<String> tags, int score, String imageUrl,
  long applyCount, long pendingCount, boolean hasAccepted
) {}
