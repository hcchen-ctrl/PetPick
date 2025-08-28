package com.petpick.petpick.DTO.shop;

import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    private String status;
    private String note;
}
