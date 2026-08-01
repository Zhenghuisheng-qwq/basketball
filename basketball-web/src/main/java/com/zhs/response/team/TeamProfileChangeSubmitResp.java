package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("提交球队资料变更响应")
public class TeamProfileChangeSubmitResp implements Serializable {

    private String changeId;
    private String changeNo;
    private Integer auditStatus;
    private String auditStatusText;
}
