package com.petpick.petpick.dto;

import lombok.Data;

@Data
public class CvsSelectRequest {
    private Integer orderId;
    private String subType;       // UNIMARTC2C / FAMIC2C / HILIFEC2C / OKMARTC2C
    private String isCollection;  // Y / N
}
