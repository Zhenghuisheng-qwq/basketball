package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketDivisionGroupTeamAssignReq;
import com.zhs.basket.dto.v4.BasketDivisionGroupUpdateReq;
import com.zhs.basket.dto.v4.BasketDivisionGroupingBatchCreateReq;
import com.zhs.basket.query.v4.BasketDivisionGroupingBatchQuery;
import com.zhs.basket.vo.v4.BasketDivisionGroupingBatchDetailVo;
import com.zhs.basket.vo.v4.BasketDivisionGroupingBatchListVo;

import java.util.List;

public interface IDivisionGroupingApplicationService {

    List<BasketDivisionGroupingBatchListVo> selectBatchList(BasketDivisionGroupingBatchQuery query);

    BasketDivisionGroupingBatchDetailVo selectBatchDetail(Long groupingBatchId);

    Long createDraftBatch(BasketDivisionGroupingBatchCreateReq req);

    void updateGroup(Long groupingBatchId, Long groupId, BasketDivisionGroupUpdateReq req);

    Long assignEventTeam(Long groupingBatchId, Long groupId, BasketDivisionGroupTeamAssignReq req);

    void removeAssignment(Long groupingBatchId, Long assignmentId);

    void lockBatch(Long groupingBatchId);

    void cancelBatch(Long groupingBatchId);
}
