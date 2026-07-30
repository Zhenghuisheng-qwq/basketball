package com.zhs.request.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("比赛异常申请提交请求")
public class PlayerMatchExceptionSubmitReq implements Serializable {

    private String exceptionType;
    private String exceptionReason;
    private List<String> materialFileIds;
    private String remark;
}
