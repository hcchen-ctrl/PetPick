package com.petpick.petpick.model;

import com.petpick.petpick.model.enums.ReportStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true) // 自動套用在所有 ReportStatus 欄位
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        // 寫回 DB：一律大寫（避免之後再出現小寫）
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String v = dbData.trim().toUpperCase(); // 讀取 DB：忽略大小寫與前後空白
        switch (v) {
            case "SUBMITTED": return ReportStatus.SUBMITTED;
            case "VERIFIED":  return ReportStatus.VERIFIED;
            case "REJECTED":  return ReportStatus.REJECTED;
            default:
                // 你也可以改成回傳預設值避免丟例外：
                // return ReportStatus.SUBMITTED;
                throw new IllegalArgumentException("Unknown ReportStatus in DB: " + dbData);
        }
    }
}

