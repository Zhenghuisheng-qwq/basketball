package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class ScheduleUnitView {
    private String unitType;
    private Long unitId;
    private Long divisionId;
    private Long stageId;
    private Long structureBatchId;
    private Integer roundNo;
    private String roundRole;
    private Integer matchOrder;
    private Long homeParticipantId;
    private Long awayParticipantId;
    private String homeParticipantName;
    private String awayParticipantName;
    private String homeSourceType;
    private String awaySourceType;
    private String homeSourceDisplay;
    private String awaySourceDisplay;
    private Integer bestOf;
    private String scheduledDate;
    private String scheduledStartTime;
    private String scheduleStatus;
    private String scheduleSource;
    private String participantResolutionStatus;
    private String publishStatus;
    private boolean canEditSchedule;
    private boolean canAdjustSchedule;
    private boolean required;
    private String matchStatus;
    private String seriesStatus;
}
