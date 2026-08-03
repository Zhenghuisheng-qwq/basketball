package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketSeriesEnabledReq {
    /** 1 启用，0 停用 */
    private Integer enabled;
}
