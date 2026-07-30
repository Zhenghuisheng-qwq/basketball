package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛异常申请提交结果")
public class PlayerMatchExceptionSubmitResultResp implements Serializable {

    private String applicationId;
    private String applicationNo;
}
