package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketHonorRecord;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.resp.HonorRecordListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketHonorRecordMapper {
    BasketHonorRecord selectById(@Param("id") Long id);

    HonorRecordListResp selectDetailById(@Param("id") Long id);

    List<HonorRecordListResp> selectList(HonorRecordQueryReq query);

    List<HonorRecordListResp> selectBySeasonAndSource(@Param("seasonId") Long seasonId,
                                                      @Param("sourceType") String sourceType);

    BasketHonorRecord selectActiveUnique(@Param("seasonId") Long seasonId,
                                         @Param("honorCategoryId") Long honorCategoryId,
                                         @Param("teamId") Long teamId,
                                         @Param("playerId") Long playerId,
                                         @Param("excludeId") Long excludeId);

    BasketHonorRecord selectBySeasonCategorySource(@Param("seasonId") Long seasonId,
                                                   @Param("honorCategoryId") Long honorCategoryId,
                                                   @Param("sourceType") String sourceType);

    int insert(BasketHonorRecord record);

    int updateById(BasketHonorRecord record);

    int updateDisplayStatusIfMatch(@Param("id") Long id,
                                   @Param("fromStatus") Integer fromStatus,
                                   @Param("toStatus") Integer toStatus);

    int updateDisplayStatusBySeasonSource(@Param("seasonId") Long seasonId,
                                          @Param("sourceType") String sourceType,
                                          @Param("fromStatuses") List<Integer> fromStatuses,
                                          @Param("toStatus") Integer toStatus);

    int softDelete(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses);

    int countActiveUnique(@Param("seasonId") Long seasonId,
                          @Param("honorCategoryId") Long honorCategoryId,
                          @Param("teamId") Long teamId,
                          @Param("playerId") Long playerId,
                          @Param("excludeId") Long excludeId);

    int revokeByGenerationBatch(@Param("generationBatchId") Long generationBatchId);

    int countBySourceHonorId(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    List<HonorRecordListResp> selectImportCandidates(@Param("tournamentId") Long tournamentId,
                                                     @Param("seasonId") Long seasonId);
}
