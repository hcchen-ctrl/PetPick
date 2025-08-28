package com.petpick.petpick.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.petpick.petpick.entity.shop.Order;
import org.springframework.data.jpa.domain.Specification;


public class AdminOrderSpecs {

    /**
     * 關鍵字：orderId / merchantTradeNo / tradeNo / receiverName / receiverPhone /
     * userId
     */
    public static Specification<Order> keyword(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }

        final String like = "%" + q.trim().toLowerCase() + "%";
        Integer idAsInt = null;
        try {
            idAsInt = Integer.valueOf(q.trim().replaceAll("\\D+", ""));
        } catch (Exception ignore) {
        }
        final Integer idMatch = idAsInt;

        return (root, cq, cb) -> cb.or(
                // #1024 這種可抓到 orderId
                (idMatch == null) ? cb.disjunction() : cb.equal(root.get("orderId"), idMatch),
                cb.like(cb.lower(root.get("merchantTradeNo")), like),
                cb.like(cb.lower(root.get("tradeNo")), like),
                cb.like(cb.lower(root.get("receiverName")), like),
                cb.like(cb.lower(root.get("receiverPhone")), like),
                cb.like(cb.lower(root.get("userId").as(String.class)), like));
    }

    /**
     * 狀態等於
     */
    public static Specification<Order> statusEq(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return (root, cq, cb) -> cb.equal(cb.lower(root.get("status")), s.toLowerCase());
    }

    /**
     * 配送方式（視你的欄位語意可自行調整）
     */
    public static Specification<Order> delivery(String d) {
        if (d == null || d.isBlank()) {
            return null;
        }
        // 假設：
        // - address：宅配（storeId IS NULL）
        // - store：到店取貨（storeId IS NOT NULL）
        // - cvs_cod：超商取貨付款（shippingType = 'CVS_COD'）
        return (root, cq, cb) -> switch (d) {
            case "address" ->
                cb.isNull(root.get("storeId"));
            case "store" ->
                cb.isNotNull(root.get("storeId"));
            case "cvs_cod" ->
                cb.equal(cb.lower(root.get("shippingType")), "cvs_cod");
            default ->
                cb.conjunction();
        };
    }

    /**
     * 訂單建立日期區間（含端點）
     */
    public static Specification<Order> dateRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }

        if (from != null && to != null) {
            LocalDateTime s = from.atStartOfDay();
            LocalDateTime e = to.atTime(LocalTime.MAX);
            return (root, cq, cb) -> cb.between(root.get("createdAt"), s, e);
        } else if (from != null) {
            LocalDateTime s = from.atStartOfDay();
            return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), s);
        } else {
            LocalDateTime e = to.atTime(LocalTime.MAX);
            return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), e);
        }
    }
}
