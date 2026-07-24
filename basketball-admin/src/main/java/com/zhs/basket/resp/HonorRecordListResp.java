package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class HonorRecordListResp {
    private Long id;
    private Long tournamentId;
    private Long seasonId;
    private String seasonName;
    private Long honorCategoryId;
    private String honorCode;
    private String honorName;
    private String honorScope;
    private String recipientType;
    private Long teamId;
    private String teamName;
    private Long playerId;
    private String playerName;
    private Long matchId;
    private String honorTitle;
    private String honorSubtitle;
    private String honorValue;
    private Date awardDate;
    private Long coverFileId;
    private String coverUrl;
    private String sourceType;
    private Long sourceId;
    private Integer displayStatus;
    private String displayStatusName;
    private Integer displayOrder;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
