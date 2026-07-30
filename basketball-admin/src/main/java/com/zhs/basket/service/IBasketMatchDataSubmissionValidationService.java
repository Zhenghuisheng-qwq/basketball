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
}
