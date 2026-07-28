package com.zhs.basket.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhs.common.core.domain.BaseEntity;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CaptainQualificationQuery extends BaseEntity {
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private String nickname;
    private Long seasonId;
    private Integer qualificationStatus;
    private String paymentReference;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date grantedTimeBegin;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date grantedTimeEnd;
}
