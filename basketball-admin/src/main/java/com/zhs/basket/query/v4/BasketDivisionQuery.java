package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketDivisionQuery {
    private Long id;
    private Long seasonId;
    private String platformCode;
    private String registrationMode;
    private String formatType;
    private String divisionStatus;
}
