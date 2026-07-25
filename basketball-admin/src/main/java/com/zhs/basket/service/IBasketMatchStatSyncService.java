package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatchOfficialResult;
import com.zhs.basket.domain.BasketStatSyncRecord;

public interface IBasketMatchStatSyncService {
    boolean tryRecordSync(BasketMatchOfficialResult officialResult, String syncType);

    BasketStatSyncRecord findByKey(String idempotencyKey);
}
