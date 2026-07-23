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
@ApiModel(value = "BasketTournamentDetailResp")
public class BasketTournamentDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "赛事编号", required = true)
        private String tournamentNo;

        @ApiModelProperty(value = "赛事名称", required = true)
        private String tournamentName;

        @ApiModelProperty(value = "赛事简称", required = true)
        private String shortName;

        @ApiModelProperty(value = "英文名称")
        private String englishName;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事标志文件主键")
        private Long logoFileId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事封面文件主键")
        private Long coverFileId;

        @ApiModelProperty(value = "赛事类型", required = true)
        private String tournamentType;

        @ApiModelProperty(value = "比赛终端", required = true)
        private String gameTerminal;

        @ApiModelProperty(value = "比赛赛制", required = true)
        private String formatCode;

        @ApiModelProperty(value = "主办方名称")
        private String organizerName;

        @ApiModelProperty(value = "赛事介绍")
        private Object description;

        @ApiModelProperty(value = "赛事状态", required = true)
        private Integer tournamentStatus;

        @ApiModelProperty(value = "公开状态", required = true)
        private Integer publicStatus;

        @ApiModelProperty(value = "展示顺序", required = true)
        private Integer displayOrder;

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
