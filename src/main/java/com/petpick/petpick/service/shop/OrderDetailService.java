package com.petpick.petpick.service.shop;

import com.petpick.petpick.DTO.shop.CreateOrderDetailRequest;
import com.petpick.petpick.DTO.shop.OrderDetailDTO;

import java.util.List;



public interface OrderDetailService {

    List<OrderDetailDTO> listByOrderId(Integer orderId);

    OrderDetailDTO get(Integer detailId);

    OrderDetailDTO create(CreateOrderDetailRequest req);

    OrderDetailDTO update(Integer detailId, CreateOrderDetailRequest req);

    void delete(Integer detailId);
}