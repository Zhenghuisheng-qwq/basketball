package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PublicHomeFocusTeamVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    private String teamLogoUrl;

    private Integer weekMatchCount;

    private Integer weekWinCount;

    private Integer weekLoseCount;

    private Integer weekPointDiff;

    private BigDecimal weekAvgPointDiff;

    private BigDecimal weekWinRate;

    private List<String> recentForm;
}
