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
 * @since 2026-07-24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketNotificationSettingResp")
public class BasketNotificationSettingResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "会员主键")
        private Long memberId;

        @ApiModelProperty(value = "系统通知开关", required = true)
        private Integer systemNotice;

        @ApiModelProperty(value = "认证通知开关", required = true)
        private Integer certificationNotice;

        @ApiModelProperty(value = "赛事通知开关", required = true)
        private Integer tournamentNotice;

        @ApiModelProperty(value = "球队通知开关", required = true)
        private Integer teamNotice;

        @ApiModelProperty(value = "公告通知开关", required = true)
        private Integer announcementNotice;

        @ApiModelProperty(value = "邮件通知开关", required = true)
        private Integer emailNotice;

        @ApiModelProperty(value = "短信通知开关", required = true)
        private Integer smsNotice;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "删除标记", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
