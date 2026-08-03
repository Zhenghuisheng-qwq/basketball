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
 * CNPA 赛事域 V4 实体：BasketSeries
 */
@Data
@ApiModel
@TableName("basket_series")
public class BasketSeries implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String seriesCode;

    private String seriesName;

    private String seriesDesc;

    private String defaultFormatType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long iconFileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long coverFileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bannerFileId;

    private Integer sortOrder;

    private Integer enabled;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
