package com.petpick.petpick.service;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;

/**
 * 寫入面（Commands）：建立訂單、更新狀態、設定物流資訊等。
 * 查詢請使用 OrderQueryService。
 *
 * 付款語意（建議）：
 * - 信用卡（CREDIT）：以「實際付款線索」為準（onPaymentSucceeded / paidAt / paidAmount）
 * - 貨到付款（COD）：僅在配達／取件完成後再補登「已付款」（通常由物流回拋或人工對帳）
 *
 * 庫存與購物車建議：
 * - checkout 時僅 reserveCart（保留），待 onPaymentSucceeded 再 commitReservation（正式扣庫存、清空購物車）
 * - 失敗／取消則 releaseReservation
 */
public interface OrderService {

    /**
     * 結帳：將使用者購物車轉成訂單與明細，回傳至少要含 orderId。
     * 建議此同時先 reserveCart（保留庫存與購物車項目），待金流成功後再 commitReservation。
     */
    OrderDTO checkout(CheckoutRequest req);

    /**
     * 一般性的狀態更新（例：Pending → Paid / Shipped / Cancelled）。
     * 多數情況建議優先使用 onPaymentSucceeded / onPaymentFailed 等專用方法以維持一致語意。
     */
    void updateStatus(Integer orderId, UpdateOrderStatusRequest req);

    /**
     * 金流回拋成功（已完成授權/請款）。
     * 建議作業：
     *  1) idempotent：若已是 Paid 直接 return
     *  2) 寫入 Payment 紀錄（gateway、tradeNo、paidAmount、payload 摘要…）
     *  3) 更新訂單狀態＝Paid，並填入 paidAt、累加 paidAmount
     *  4) commitReservation（正式扣庫存、清空購物車）
     */
    void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount);

    /**
     * 金流回拋失敗或使用者取消。
     * 建議作業：
     *  1) 若尚未出貨，更新狀態＝Failed/Cancelled，保存失敗原因
     *  2) releaseReservation（釋放購物車保留、恢復庫存）
     */
    void onPaymentFailed(Integer orderId, String reason);

    /**
     * （選用）設定超商門市資訊：C2C 選店完成後寫入。
     */
    void setStoreInfo(Integer orderId, String brandCodeOrLabel, String storeId, String storeName, String storeAddress);

    /**
     * （選用）設定宅配/物流資訊：建立託運成功後寫入（託運單號、追蹤碼）。
     */
    void setLogisticsInfo(Integer orderId, String logisticsId, String trackingNo);

    /**
     * 取消訂單（可帶原因）。應同時 releaseReservation。
     */
    void cancel(Integer orderId, String reason);

    /**
     * 保留購物車內容與庫存（在 checkout 內部或緊接著呼叫）。
     * 典型作法：把 cart items 複製到 order_items，並將 cart 標記 reserved_by_order_id，
     * 但「不」真正刪除 cart（或不扣庫存）。
     */
    void reserveCart(Integer orderId);

    /**
     * 付款成功後，正式提交保留（扣庫存、清空購物車）。
     */
    void commitReservation(Integer orderId);

    /**
     * 付款失敗／取消時，釋放保留（恢復庫存、清除保留標記）。
     */
    void releaseReservation(Integer orderId);

    // ---- 相容舊呼叫：無原因的取消 ----

    /**
     * （相容用）取消訂單，不帶原因。
     * 預設委派至 {@link #cancel(Integer, String)}，讓舊程式碼可無痛沿用。
     */
    default void cancel(Integer orderId) {
        cancel(orderId, null);
    }
}