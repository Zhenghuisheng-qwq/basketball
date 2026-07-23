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
@ApiModel(value = "BasketTeamCaptainHistoryResp")
public class BasketTeamCaptainHistoryResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "原队长会员主键")
        private Long oldCaptainMemberId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "原队长球员主键")
        private Long oldCaptainPlayerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "新队长会员主键")
        private Long newCaptainMemberId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "新队长球员主键")
        private Long newCaptainPlayerId;

        @ApiModelProperty(value = "变更类型", required = true)
        private String changeType;

        @ApiModelProperty(value = "变更原因")
        private String changeReason;

        @ApiModelProperty(value = "生效时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date effectiveTime;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "操作人主键")
        private Long operatorId;

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
