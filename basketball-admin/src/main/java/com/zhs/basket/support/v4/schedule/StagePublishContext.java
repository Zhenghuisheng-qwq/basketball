package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.basket.vo.v4.StageScheduleSummaryVo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StagePublishContext {
    private BasketDivision division;
    private BasketDivisionStage stage;
    private BasketDivisionFormatSnapshot ruleVersion;
    private BasketDivisionRosterVersion rosterVersion;
    private BasketScheduleBatch structureBatch;
    private BasketStagePairingExecution pairingExecution;
    private StageScheduleSummaryVo scheduleSummary;
    private List<ScheduleUnitView> units = new ArrayList<>();
    private String publishStatus;
    private Long currentOperator;
    private boolean firstStage;
}
