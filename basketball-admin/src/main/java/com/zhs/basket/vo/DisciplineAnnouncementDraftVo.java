package com.zhs.basket.vo;

import lombok.Data;

@Data
public class DisciplineAnnouncementDraftVo {
    private Long disciplineId;
    private Long announcementId;
    private String title;
    private String contentText;
    private Integer announcementStatus;
    private String announcementStatusName;
    private boolean created;
}
