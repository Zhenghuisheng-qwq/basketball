package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.vo.StatChangeRecordVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketStatChangeRecordMapper {
    int insert(BasketStatChangeRecord record);
    int batchInsert(@Param("list") List<BasketStatChangeRecord> list);
    List<StatChangeRecordVo> selectByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
