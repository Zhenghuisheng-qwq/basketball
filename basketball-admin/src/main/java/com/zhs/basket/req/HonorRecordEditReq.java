package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class HonorRecordEditReq {
    private Long honorCategoryId;
    private String honorCode;
    private Long teamId;
    private Long playerId;
    private Long matchId;
    private String honorTitle;
    private String honorTitleEn;
    private String honorSubtitle;
    private String honorSubtitleEn;
    private String honorValue;
    private Date awardDate;
    private Long coverFileId;
    private Integer displayOrder;
    private String remark;
}
