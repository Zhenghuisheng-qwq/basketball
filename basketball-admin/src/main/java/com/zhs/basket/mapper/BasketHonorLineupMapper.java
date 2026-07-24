package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketHonorLineup;
import com.zhs.basket.resp.HonorLineupResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketHonorLineupMapper {
    List<HonorLineupResp> selectByHonorRecordId(@Param("honorRecordId") Long honorRecordId);

    int softDeleteByHonorRecordId(@Param("honorRecordId") Long honorRecordId);

    int batchInsert(@Param("list") List<BasketHonorLineup> list);
}
