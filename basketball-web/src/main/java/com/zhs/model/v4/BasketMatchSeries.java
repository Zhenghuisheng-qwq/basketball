package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("basket_match_series")
public class BasketMatchSeries implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long divisionId;
    private Long stageId;
    private Long scheduleBatchId;
    private String seriesCode;
    private Integer roundNo;
    private Integer bracketSlot;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private String homeSourceType;
    private String awaySourceType;
    private String homeSourceDisplay;
    private String awaySourceDisplay;
    private Integer bestOf;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private Long winnerEventTeamId;
    private String seriesStatus;
    private String resolutionType;
    private Integer isDelete;
}
