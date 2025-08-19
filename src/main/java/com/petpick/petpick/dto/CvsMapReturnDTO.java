package com.petpick.petpick.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CvsMapReturnDTO {
    // ── NewebPay 常見欄位 ─────────────────────────────────
    private String MerchantID;
    private String MerchantTradeNo;   // 你送出地圖用的識別碼
    private String LogisticsType;     // CVS
    private String LogisticsSubType;  // UNIMART / FAMI / HILIFE / OK
    private String IsCollection;      // Y / N

    private String StoreID;           // 門市代碼
    private String StoreName;         // 門市名稱
    private String StoreAddress;      // 門市地址
    private String StoreTel;          // 門市電話

    private String ExtraData;         // 你自行透過表單帶回的資料（例如 orderId）
    private String CheckValue;        // 藍新簽章

    /**
     * 方便從回拋的參數 Map 建立 DTO。
     * 同時支援舊的綠界鍵名（CVSStoreID/Name/Address/Telephone）做相容。
     */
    public static CvsMapReturnDTO from(Map<String, String> form) {
        CvsMapReturnDTO dto = new CvsMapReturnDTO();

        dto.setMerchantID(val(form, "MerchantID"));
        dto.setMerchantTradeNo(val(form, "MerchantTradeNo"));
        dto.setLogisticsType(val(form, "LogisticsType"));
        dto.setLogisticsSubType(val(form, "LogisticsSubType"));
        dto.setIsCollection(val(form, "IsCollection"));

        // 門市資訊（優先取藍新鍵名，沒有就吃綠界舊鍵）
        dto.setStoreID(or(val(form, "StoreID"), val(form, "CVSStoreID")));
        dto.setStoreName(or(val(form, "StoreName"), val(form, "CVSStoreName")));
        dto.setStoreAddress(or(val(form, "StoreAddress"), val(form, "CVSAddress")));
        dto.setStoreTel(or(val(form, "StoreTel"), val(form, "CVSTelephone"), val(form, "CVSPhone")));

        dto.setExtraData(or(val(form, "ExtraData"), val(form, "ExtraData1"), val(form, "ExtraData2")));
        dto.setCheckValue(or(val(form, "CheckValue"), val(form, "CheckCode"), val(form, "CheckMacValue")));

        return dto;
    }

    // 取值工具：避免 NPE
    private static String val(Map<String, String> m, String key) {
        String v = m.get(key);
        return v == null ? "" : v;
    }

    private static String or(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
