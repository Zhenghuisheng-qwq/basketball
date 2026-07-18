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
@ApiModel(value = "BasketPlayerDetailResp")
public class BasketPlayerDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "网页账号编号")
        private Long webUserId;

        @ApiModelProperty(value = "球员编号", required = true)
        private String playerNo;

        @ApiModelProperty(value = "邮箱账号", required = true)
        private String email;

        @ApiModelProperty(value = "球员昵称", required = true)
        private String nickname;

        @ApiModelProperty(value = "头像地址")
        private String avatarUrl;

        @ApiModelProperty(value = "比赛终端", required = true)
        private String terminalType;

        @ApiModelProperty(value = "性别")
        private String gender;

        @ApiModelProperty(value = "年龄")
        private Integer age;

        @ApiModelProperty(value = "所在城市")
        private String city;

        @ApiModelProperty(value = "游戏账号")
        private String gameAccount;

        @ApiModelProperty(value = "游戏昵称")
        private String gameRoleName;

        @ApiModelProperty(value = "常用位置")
        private String mainPosition;

        @ApiModelProperty(value = "打法特点")
        private String playStyle;

        @ApiModelProperty(value = "个人简介")
        private String profileDesc;

        @ApiModelProperty(value = "队长标志", required = true)
        private String captainFlag;

        @ApiModelProperty(value = "球员状态", required = true)
        private String playerStatus;

        @ApiModelProperty(value = "注册时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date registerTime;

        @ApiModelProperty(value = "最后登录时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date lastLoginTime;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新者")
        private String updateBy;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "备注")
        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
