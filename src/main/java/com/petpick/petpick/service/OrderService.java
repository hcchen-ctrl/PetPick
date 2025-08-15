package com.petpick.petpick.service;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;

/**
 * 寫入面（Commands）：建立訂單、更新狀態、設定物流資訊…等。
 * 不提供列表/單筆查詢（改由 OrderQueryService 負責）。
 */
public interface OrderService {

    /**
     * 結帳：將使用者購物車轉成訂單與明細，回傳最少需含 orderId。
     * 你的 checkout.html 只要取得 orderId 即可分流到金流/物流。
     */
    OrderDTO checkout(CheckoutRequest req);

    /**
     * 更新訂單狀態（例如：Pending → Paid / Shipped / Cancelled）
     * 綠界 ReturnURL 回拋成功時會呼叫此方法把狀態改為 Paid。
     */
    void updateStatus(Integer orderId, UpdateOrderStatusRequest req);

    /**
     * （選用）設定超商門市資訊：C2C 選店完成後寫入
     */
    void setStoreInfo(Integer orderId, String storeId, String storeName, String storeAddress);

    /**
     * （選用）設定宅配/物流資訊：建立託運成功後寫入
     */
    void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo);

    /**
     * （選用）取消訂單
     */
    void cancel(Integer orderId);
}
