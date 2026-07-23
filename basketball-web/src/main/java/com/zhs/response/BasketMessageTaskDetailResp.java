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
@ApiModel(value = "BasketMessageTaskDetailResp")
public class BasketMessageTaskDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "任务编号", required = true)
        private String taskNo;

        @ApiModelProperty(value = "消息类型", required = true)
        private String messageType;

        @ApiModelProperty(value = "消息标题", required = true)
        private String title;

        @ApiModelProperty(value = "消息内容", required = true)
        private Object contentText;

        @ApiModelProperty(value = "关联业务类型")
        private String bizType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "关联业务主键")
        private Long bizId;

        @ApiModelProperty(value = "发送范围", required = true)
        private String sendScope;

        @ApiModelProperty(value = "发送状态", required = true)
        private Integer sendStatus;

        @ApiModelProperty(value = "计划发送时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date scheduledTime;

        @ApiModelProperty(value = "实际发送时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date sentTime;

        @ApiModelProperty(value = "接收总数", required = true)
        private Integer totalCount;

        @ApiModelProperty(value = "成功数量", required = true)
        private Integer successCount;

        @ApiModelProperty(value = "失败数量", required = true)
        private Integer failCount;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "创建人主键")
        private Long creatorId;

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
