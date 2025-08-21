package com.petpick.petpick.service;

import java.util.List;

import com.petpick.petpick.dto.CreateOrderDetailRequest;
import com.petpick.petpick.dto.OrderDetailDTO;

public interface OrderDetailService {

    List<OrderDetailDTO> listByOrderId(Integer orderId);

    OrderDetailDTO get(Integer detailId);

    OrderDetailDTO create(CreateOrderDetailRequest req);

    OrderDetailDTO update(Integer detailId, CreateOrderDetailRequest req);

    void delete(Integer detailId);
}