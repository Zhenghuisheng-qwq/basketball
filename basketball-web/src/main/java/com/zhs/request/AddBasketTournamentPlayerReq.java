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
public class AddBasketTournamentPlayerReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long tournamentTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourcePlayerId;

        private String playerNo;

        private String playerName;

        private String avatarUrl;

        private String gameAccount;

        private String gameRoleName;

        private Integer jerseyNo;

        private String positionName;

        private String starterFlag;

        private String captainFlag;

        private String playerStatus;

        private String createBy;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
