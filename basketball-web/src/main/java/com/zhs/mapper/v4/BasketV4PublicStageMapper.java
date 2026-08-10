package com.zhs.mapper.v4;

import com.zhs.response.v4.publicapi.V4PublicStageItemResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4PublicStageMapper {

    List<V4PublicStageItemResp> selectStagesByDivisionId(@Param("divisionId") Long divisionId);

    V4PublicStageItemResp selectStageById(@Param("stageId") Long stageId);

    List<V4PublicStandingItemResp> selectStandingsByStageScope(@Param("stageId") Long stageId,
                                                                 @Param("groupScopeKey") Long groupScopeKey);

    List<Long> selectGroupScopeKeysByStageId(@Param("stageId") Long stageId);
}
