package com.petpick.petpick.service;

import com.petpick.petpick.dto.CheckoutRequest;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.dto.UpdateOrderStatusRequest;

/**
 * 寫入面（Commands）：建立訂單、更新狀態、設定物流資訊等。 查詢面請改由 OrderQueryService 處理。
 */
public interface OrderService {
    void cancel(Integer orderId);
    /**
     * 結帳：將使用者購物車轉成訂單與明細，回傳至少要含 orderId。 建議在此同時「保留購物車項目（reserve）」而不是直接清空，
     * 等付款成功（onPaymentSucceeded）再正式扣庫存與清空購物車。
     */
    OrderDTO checkout(CheckoutRequest req);

    /**
     * 一般性的狀態更新（例：Pending→Paid / Shipped / Cancelled）。 多數情況建議優先使用下方專用方法
     * onPaymentSucceeded / onPaymentFailed。
     */
    void updateStatus(Integer orderId, UpdateOrderStatusRequest req);

    /**
     * 金流回拋成功（已完成授權/請款）。 建議作業： 1) idempotent：若已是 Paid 直接 return 2) 寫入 Payment
     * 紀錄（gateway、tradeNo、paidAmount、payload 摘要…） 3) 更新訂單狀態＝Paid，填入 paidAt 4)
     * commitReservation（正式扣庫存、清空購物車）
     */
    void onPaymentSucceeded(Integer orderId, String gateway, String tradeNo, int paidAmount);

    /**
     * 金流回拋失敗或使用者取消。 建議作業： 1) 若尚未出貨，更新狀態＝Failed/Cancelled，保存失敗原因 2)
     * releaseReservation（釋放購物車保留、恢復庫存）
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
     * 保留購物車內容與庫存（在 checkout 內部或緊接著呼叫）。 典型作法：把 cart items copy 到 order_items，並把
     * cart 標記 reserved_by_order_id， 但「不」真正刪除 cart（或不扣庫存）。
     */
    void reserveCart(Integer orderId);

    /**
     * 付款成功後，正式提交保留（扣庫存、清空購物車）。
     */
    void commitReservation(Integer orderId);

    /**
     * 付款失敗／取消時，釋放保留（恢復庫存、保留的 cart 標記清除）。
     */
    void releaseReservation(Integer orderId);

}
