package com.zhs.response.participation;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("阵容变更申请提交结果")
public class RosterChangeSubmitResp implements Serializable {

    private String requestId;
    private String requestNo;
}
