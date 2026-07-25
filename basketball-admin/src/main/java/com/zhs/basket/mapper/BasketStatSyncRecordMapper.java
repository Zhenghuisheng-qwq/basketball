package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketStatSyncRecord;

public interface BasketStatSyncRecordMapper {
    int insert(BasketStatSyncRecord row);

    BasketStatSyncRecord selectByIdempotencyKey(String idempotencyKey);
}
