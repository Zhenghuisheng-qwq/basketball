package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;

import java.util.List;

public interface IBasketMatchDataSubmissionValidationService {
    void validateSubmissionData(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPeriod> periods,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            List<BasketMatchDataSubmissionPlayerStat> playerStats,
            List<BasketMatchDataSubmissionMaterial> materials);

    /** 正式数据修正校验（不要求材料）。 */
    void validateCorrectionData(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPeriod> periods,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            List<BasketMatchDataSubmissionPlayerStat> playerStats);
}
