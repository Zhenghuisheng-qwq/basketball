package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("发送邮箱验证码响应")
public class EmailCodeSendResp implements Serializable {

    @ApiModelProperty("业务流水号")
    private String bizNo;

    @ApiModelProperty("有效秒数")
    private Long expireSeconds;

    @ApiModelProperty("重发间隔秒数")
    private Long resendSeconds;

    /**
     * 兼容字段：不再向客户端暴露 Dev Bypass 状态（始终为 null）。
     * Bypass 仅由服务端环境变量 CNPA_MAIL_DEV_BYPASS 控制，只写服务端日志。
     */
    @ApiModelProperty(hidden = true)
    private Boolean devBypass;
}
