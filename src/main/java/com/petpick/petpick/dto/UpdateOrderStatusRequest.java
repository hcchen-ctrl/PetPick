package com.petpick.petpick.dto;

import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    private String status;
    private String note;
}
