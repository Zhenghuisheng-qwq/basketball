package com.zhs.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
public class UpdateBasketDisciplineRecordReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        private String recordNo;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long matchId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long memberId;

        private String disciplineType;

        private String title;

        private Object reasonText;

        private Object penaltyContent;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date effectiveStartTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date effectiveEndTime;

        private Integer disciplineStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long decisionBy;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date decisionTime;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long announcementId;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
