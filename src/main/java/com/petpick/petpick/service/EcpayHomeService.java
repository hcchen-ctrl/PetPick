package com.petpick.petpick.service;

import java.util.Map;

public interface EcpayHomeService {

    /**
     * 綠界物流「宅配建單」（測試站）
     * @param params 需含至少：MerchantID, LogisticsType=HOME, LogisticsSubType(如 TCAT), Sender/Receiver 資訊等
     *              （本專案會在實作層補齊必要欄位與 CheckMacValue）
     * @return 綠界回傳的欄位（JSON 或 form 格式解析後的 Map）
     */
    Map<String, String> createHomeShipment(Map<String, String> params);
}
