package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketCaptainGrantAdjustReq {
    /** AUTO_EVENT 或 CUSTOM */
    private String validToMode;
    private Date validTo;
}
