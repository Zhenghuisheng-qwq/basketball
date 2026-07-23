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
@ApiModel(value = "BasketCaptainQualificationResp")
public class BasketCaptainQualificationResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "资格编号", required = true)
        private String qualificationNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "会员主键")
        private Long memberId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员主键")
        private Long playerId;

        @ApiModelProperty(value = "资格状态", required = true)
        private Integer qualificationStatus;

        @ApiModelProperty(value = "当前有效标记", required = true)
        private Integer isCurrent;

        @ApiModelProperty(value = "线下缴费确认状态", required = true)
        private Integer paymentConfirmed;

        @ApiModelProperty(value = "线下缴费参考号")
        private String paymentReference;

        @ApiModelProperty(value = "授予原因")
        private String grantReason;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "授予人主键")
        private Long grantedBy;

        @ApiModelProperty(value = "授予时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date grantedTime;

        @ApiModelProperty(value = "生效时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date validFrom;

        @ApiModelProperty(value = "失效时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date validTo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "取消人主键")
        private Long revokedBy;

        @ApiModelProperty(value = "取消时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date revokedTime;

        @ApiModelProperty(value = "取消原因")
        private String revokeReason;

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
