package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.*;
import com.zhs.basket.query.v4.BasketPersonalDrawBatchQuery;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchDetailVo;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchListVo;
import com.zhs.basket.vo.v4.BasketPersonalDrawTeamVo;

import java.util.List;

public interface IPersonalTeamFormationApplicationService {
    List<BasketPersonalDrawBatchListVo> selectBatchList(BasketPersonalDrawBatchQuery query);

    BasketPersonalDrawBatchDetailVo selectBatchDetail(Long drawBatchId);

    Long createDraftBatch(BasketPersonalDrawBatchCreateReq req);

    Long createTeam(Long drawBatchId, BasketPersonalDrawTeamCreateReq req);

    Long addAssignment(Long drawBatchId, Long drawTeamId, BasketPersonalDrawAssignmentCreateReq req);

    void removeAssignment(Long drawBatchId, Long assignmentId);

    void confirmBatch(Long drawBatchId, BasketPersonalDrawBatchConfirmReq req);

    void cancelBatch(Long drawBatchId);

    void bindLongTermTeam(Long drawTeamId, BasketPersonalFormationBindTeamReq req);

    Long confirmFormation(Long drawTeamId);
}
