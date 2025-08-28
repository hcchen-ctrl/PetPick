package com.petpick.petpick.service.shop;

public interface EcpayService {
    String buildAioCheckoutForm(Integer orderId);
    String buildAioCheckoutForm(Integer orderId, String origin);
}
