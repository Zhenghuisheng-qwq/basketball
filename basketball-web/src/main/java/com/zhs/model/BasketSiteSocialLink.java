package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_site_social_link")
@ApiModel("官网社群与媒体入口")
public class BasketSiteSocialLink implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("平台编码")
    private String platformCode;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("描述")
    private String subtitle;

    @ApiModelProperty("按钮文案")
    private String buttonText;

    @ApiModelProperty("跳转链接")
    private String jumpUrl;

    @ApiModelProperty("排序")
    private Integer sortOrder;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    @ApiModelProperty("删除标记")
    private Integer isDelete;
}
