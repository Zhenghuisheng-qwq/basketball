package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.query.v4.BasketScheduleBatchQuery;
import com.zhs.basket.vo.v4.BasketScheduleBatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketScheduleBatchMapper {
    BasketScheduleBatch selectById(@Param("id") Long id);

    List<BasketScheduleBatch> selectList(BasketScheduleBatch query);

    List<BasketScheduleBatchListVo> selectVoList(BasketScheduleBatchQuery query);

    BasketScheduleBatch selectDraftByDivisionId(@Param("divisionId") Long divisionId);

    BasketScheduleBatch selectPublishedByDivisionId(@Param("divisionId") Long divisionId);

    BasketScheduleBatch selectDraftByStageId(@Param("stageId") Long stageId);

    BasketScheduleBatch selectPublishedByStageId(@Param("stageId") Long stageId);

    BasketScheduleBatch selectActiveByStageId(@Param("stageId") Long stageId);

    List<BasketScheduleBatch> selectByDivisionIds(@Param("divisionIds") List<Long> divisionIds);

    int countByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketScheduleBatch entity);

    int updateById(BasketScheduleBatch entity);

    int softDelete(@Param("id") Long id);

    int markStructureStaleById(@Param("id") Long id);

    int markStructureStaleByRuleVersionId(@Param("ruleVersionId") Long ruleVersionId);

    int markStructureStaleByRosterVersionId(@Param("rosterVersionId") Long rosterVersionId);

    int incrementScheduleRevision(@Param("id") Long id);
}
