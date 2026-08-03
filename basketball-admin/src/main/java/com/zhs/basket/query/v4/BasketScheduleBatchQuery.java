package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketScheduleBatchQuery {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private String formatType;
    private String generateStatus;
    private String publishStatus;
}
