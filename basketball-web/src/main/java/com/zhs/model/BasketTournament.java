package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.alibaba.fastjson2.JSONObject;


/**
 * 赛事主表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTournament implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "赛事编号")
    private String tournamentNo;

    @ApiModelProperty(value = "赛事名称")
    private String tournamentName;

    @ApiModelProperty(value = "赛事简称")
    private String shortName;

    @ApiModelProperty(value = "英文名称")
    private String englishName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事标志文件主键")
    private Long logoFileId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事封面文件主键")
    private Long coverFileId;


    @ApiModelProperty(value = "赛事类型")
    private String tournamentType;

    @ApiModelProperty(value = "比赛终端")
    private String gameTerminal;

    @ApiModelProperty(value = "比赛赛制")
    private String formatCode;

    @ApiModelProperty(value = "主办方名称")
    private String organizerName;

    @ApiModelProperty(value = "赛事介绍")
    private Object description;

    @ApiModelProperty(value = "赛事状态")
    private Integer tournamentStatus;

    @ApiModelProperty(value = "公开状态")
    private Integer publicStatus;

    @ApiModelProperty(value = "展示顺序")
    private Integer displayOrder;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "删除标记")
    private Integer isDelete;


//===========================自定义字段=================================

}
