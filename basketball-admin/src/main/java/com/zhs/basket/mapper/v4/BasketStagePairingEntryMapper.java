package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStagePairingEntry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketStagePairingEntryMapper {

    List<BasketStagePairingEntry> selectByExecutionId(@Param("executionId") Long executionId);

    int batchInsert(@Param("list") List<BasketStagePairingEntry> list);

    int updateResolution(BasketStagePairingEntry entity);

    int deleteByExecutionId(@Param("executionId") Long executionId);
}
