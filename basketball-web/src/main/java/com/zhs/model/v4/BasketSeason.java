package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketSeason
 */
@Data
@ApiModel
@TableName("basket_season")
public class BasketSeason implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    private Integer seasonYear;

    private String seasonName;

    private String displayTitle;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long coverFileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bannerFileId;

    private String registrationMode;

    private String formatType;

    private Date registrationStartTime;

    private Date registrationEndTime;

    private Date matchStartTime;

    private Date matchEndTime;

    private String publishStatus;

    private String seasonStatus;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
