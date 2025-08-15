package com.petpick.petpick.service;

public interface EcpayService {

    String buildAioCheckoutForm(Integer orderId);

    String buildAioCheckoutForm(Integer orderId, String origin); // 可指定導回網址的 domain
}
