package com.petpick.petpick.service.shop.impl;

import java.util.List;

import com.petpick.petpick.DTO.shop.CreateOrderDetailRequest;
import com.petpick.petpick.DTO.shop.OrderDetailDTO;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.entity.shop.OrderDetail;
import com.petpick.petpick.entity.shop.Product;
import com.petpick.petpick.repository.shop.OrderDetailRepository;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.repository.shop.ProductRepository;
import com.petpick.petpick.service.shop.OrderDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderDetailServiceImpl implements OrderDetailService {

    private final OrderDetailRepository detailRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailDTO> listByOrderId(Integer orderId) {
        return detailRepo.findDTOsByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDTO get(Integer detailId) {
        OrderDetail d = detailRepo.findById(detailId).orElseThrow();
        return toDto(d);
    }

    @Override
    @Transactional
    public OrderDetailDTO create(CreateOrderDetailRequest req) {
        Order order = orderRepo.findById(req.getOrderId()).orElseThrow();
        Product product = productRepo.findById(req.getProductId()).orElseThrow();

        int qty  = req.getQuantity()  == null ? 0 : req.getQuantity();
        int unit = req.getUnitPrice() == null ? 0 : req.getUnitPrice();

        OrderDetail d = new OrderDetail();
        d.setOrder(order);
        d.setProduct(product);
        d.setQuantity(qty);
        d.setUnitPrice(unit);         // 整數單價
        recalcSubtotalSafe(d);        // 會把 subtotal 設好（若無 recomputeSubtotal 也可）
        d = detailRepo.save(d);

        return toDto(d);
    }

    @Override
    @Transactional
    public OrderDetailDTO update(Integer detailId, CreateOrderDetailRequest req) {
        OrderDetail d = detailRepo.findById(detailId).orElseThrow();

        if (req.getQuantity()  != null) d.setQuantity(req.getQuantity());
        if (req.getUnitPrice() != null) d.setUnitPrice(req.getUnitPrice());

        recalcSubtotalSafe(d);
        d = detailRepo.save(d);
        return toDto(d);
    }

    @Override
    @Transactional
    public void delete(Integer detailId) {
        detailRepo.deleteById(detailId);
    }

    // -------- helpers --------

    /** 優先呼叫實體的 recomputeSubtotal()；若沒有就以 int 相乘計出小計（含溢位保護） */
    private void recalcSubtotalSafe(OrderDetail d) {
        try {
            OrderDetail.class.getMethod("recomputeSubtotal").invoke(d);
        } catch (Exception ignore) {
            int up = d.getUnitPrice() == null ? 0 : d.getUnitPrice();
            int q  = d.getQuantity()  == null ? 0 : d.getQuantity();
            long s = (long) up * (long) q; // 先 long 避免溢位
            if (s > Integer.MAX_VALUE) s = Integer.MAX_VALUE;
            if (s < 0) s = 0;
            d.setSubtotal((int) s);
        }
    }

    private OrderDetailDTO toDto(OrderDetail d) {
        return new OrderDetailDTO(
            d.getId(),
            d.getOrder()   != null ? d.getOrder().getOrderId()       : null,
            d.getProduct() != null ? d.getProduct().getProductId()   : null,
            d.getProduct() != null ? d.getProduct().getPname()       : null,
            d.getUnitPrice(),
            d.getQuantity(),
            d.getSubtotal()
        );
    }
}