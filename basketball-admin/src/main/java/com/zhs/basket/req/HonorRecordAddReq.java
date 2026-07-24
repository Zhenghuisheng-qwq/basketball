package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class HonorRecordAddReq {
    private Long tournamentId;
    private Long seasonId;
    private Long honorCategoryId;
    private String honorCode;
    private Long teamId;
    private Long playerId;
    private Long matchId;
    private String honorTitle;
    private String honorSubtitle;
    private String honorValue;
    private Date awardDate;
    private Long coverFileId;
    private Integer displayOrder;
    private String remark;
}
