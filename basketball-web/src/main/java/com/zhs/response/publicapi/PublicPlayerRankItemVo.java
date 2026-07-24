package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel("公开球员排行项")
public class PublicPlayerRankItemVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String rankType;
    private String rankTypeName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    private String playerName;
    private String avatarUrl;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    private String teamName;
    private Integer rankNo;
    private Integer previousRank;
    private BigDecimal statValue;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date snapshotDate;
}
