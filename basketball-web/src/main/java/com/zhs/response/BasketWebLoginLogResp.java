package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.alibaba.fastjson2.JSONObject;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketWebLoginLogResp")
public class BasketWebLoginLogResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "网页账号编号")
        private Long webUserId;

        @ApiModelProperty(value = "邮箱账号")
        private String email;

        @ApiModelProperty(value = "登录地址")
        private String loginIp;

        @ApiModelProperty(value = "浏览器")
        private String browser;

        @ApiModelProperty(value = "操作系统")
        private String osName;

        @ApiModelProperty(value = "登录状态", required = true)
        private String loginStatus;

        @ApiModelProperty(value = "登录提示")
        private String loginMsg;

        @ApiModelProperty(value = "登录时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date loginTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
