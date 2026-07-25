package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatchOfficialResult;
import com.zhs.basket.domain.BasketStatSyncRecord;
import com.zhs.basket.mapper.BasketStatSyncRecordMapper;
import com.zhs.basket.service.IBasketMatchStatSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BasketMatchStatSyncServiceImpl implements IBasketMatchStatSyncService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketStatSyncRecordMapper statSyncRecordMapper;

    @Override
    public boolean tryRecordSync(BasketMatchOfficialResult officialResult, String syncType) {
        String key = buildKey(officialResult.getMatchId(), officialResult.getId(), syncType);
        if (statSyncRecordMapper.selectByIdempotencyKey(key) != null) {
            return false;
        }
        Date now = new Date();
        BasketStatSyncRecord row = new BasketStatSyncRecord();
        row.setMatchId(officialResult.getMatchId());
        row.setOfficialResultId(officialResult.getId());
        row.setSyncType(syncType);
        row.setSyncStatus("SUCCESS");
        row.setIdempotencyKey(key);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        try {
            statSyncRecordMapper.insert(row);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public BasketStatSyncRecord findByKey(String idempotencyKey) {
        return statSyncRecordMapper.selectByIdempotencyKey(idempotencyKey);
    }

    public static String buildKey(Long matchId, Long officialResultId, String syncType) {
        return "MATCH:" + matchId + ":OR:" + officialResultId + ":TYPE:" + syncType;
    }
}
