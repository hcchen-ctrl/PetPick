package com.petpick.petpick.service;

public interface EcpayService {
    String buildAioCheckoutForm(Integer orderId);
    String buildAioCheckoutForm(Integer orderId, String origin);
}
