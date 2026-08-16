package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketDisciplineExecutionDetail;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PenaltyExecutionResultVo {
    private String executionStatus;
    private Long eventTeamId;
    private String eventTeamName;
    private Long divisionId;
    private Long seasonId;
    private String seasonName;
    private String eventTeamStatus;
    private int futureMatchForfeitCount;
    private int seriesForfeitCount;
    private int qualificationRemovedCount;
    private int qualificationBlockedCount;
    private int warningCount;
    private Date executedTime;
    private String errorMessage;
    private List<Long> forfeitedMatchIds = new ArrayList<>();
    private List<Long> forfeitedSeriesIds = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<BasketDisciplineExecutionDetail> details = new ArrayList<>();
}
