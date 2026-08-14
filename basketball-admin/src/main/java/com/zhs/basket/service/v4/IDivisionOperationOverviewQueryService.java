package com.zhs.basket.service.v4;

import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.vo.v4.DivisionSelectorItemVo;
import com.zhs.basket.vo.v4.DivisionWorkflowOverviewVo;

import java.util.List;

public interface IDivisionOperationOverviewQueryService {

    List<DivisionSelectorItemVo> listSelector(BasketDivisionQuery query, String entry);

    DivisionWorkflowOverviewVo getOverview(Long divisionId);
}
