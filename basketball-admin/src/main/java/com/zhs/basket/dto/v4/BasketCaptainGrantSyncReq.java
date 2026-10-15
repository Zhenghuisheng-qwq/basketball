package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketCaptainGrantSyncReq {
    private List<Long> ids;
}
