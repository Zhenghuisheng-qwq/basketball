package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("公开球队详情")
public class PublicTeamDetailVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String englishName;
    private String logoUrl;
    private String regionCode;
    private String regionName;
    private String gameTerminal;
    private Object description;
    private Integer teamStatus;
    private Integer currentMemberCount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date establishedDate;
}
