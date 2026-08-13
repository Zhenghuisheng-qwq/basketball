package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionGroup;
import com.zhs.basket.domain.v4.BasketDivisionGroupingBatch;
import com.zhs.basket.domain.v4.BasketDivisionGroupTeam;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶段赛程生成执行上下文。
 */
@Data
public class StageExecutionContext {

    private BasketDivision division;
    private BasketDivisionStage stage;
    private List<BasketDivisionStageTeam> stageTeams = new ArrayList<>();
    private BasketDivisionGroupingBatch groupingBatch;
    private List<BasketDivisionGroup> groups = new ArrayList<>();
    private List<BasketDivisionGroupTeam> groupTeams = new ArrayList<>();
    private Integer executionVersion;
    private StructureVersionContext versionContext;
    private Long pairingExecutionId;
    private BracketSlotPlan bracketSlotPlan;
}
