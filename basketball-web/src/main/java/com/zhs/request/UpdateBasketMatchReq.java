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
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class UpdateBasketMatchReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long groupId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long roundId;

        private String matchNo;

        private String matchName;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date matchTime;

        private String roomName;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long homeTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long awayTeamId;

        private Integer homeScore;

        private Integer awayScore;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long winnerTeamId;

        private String matchStatus;

        private String dataStatus;

        private String createBy;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        private String updateBy;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
