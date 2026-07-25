package com.zhs.response.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("Open 报名列表项")
public class OpenRegistrationItemResp implements Serializable {

    private String registrationId;
    private String registrationNo;
    private String seasonId;
    private String seasonName;
    private String tournamentName;
    private String platform;
    private String displayGameId;
    private Integer registrationStatus;
    private String registrationStatusText;
    private Boolean canEdit;
    private Boolean canSubmit;
    private Boolean canWithdraw;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
