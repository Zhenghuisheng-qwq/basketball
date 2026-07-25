package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketHonorGenerationBatch;
import com.zhs.basket.resp.HonorGenerationBatchResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketHonorGenerationBatchMapper {

    BasketHonorGenerationBatch selectById(@Param("id") Long id);

    BasketHonorGenerationBatch selectCurrentBySeason(@Param("seasonId") Long seasonId);

    List<HonorGenerationBatchResp> selectBySeason(@Param("seasonId") Long seasonId);

    int countByNoPrefix(@Param("prefix") String prefix);

    int clearCurrentBySeason(@Param("seasonId") Long seasonId);

    int insert(BasketHonorGenerationBatch batch);

    int updateStatus(@Param("id") Long id,
                     @Param("batchStatus") String batchStatus,
                     @Param("honorCount") Integer honorCount,
                     @Param("errorMessage") String errorMessage,
                     @Param("currentFlag") Integer currentFlag);
}
