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
@ApiModel(value = "BasketVenueResp")
public class BasketVenueResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "场地编号", required = true)
        private String venueNo;

        @ApiModelProperty(value = "场地名称", required = true)
        private String venueName;

        @ApiModelProperty(value = "省份编码")
        private String provinceCode;

        @ApiModelProperty(value = "省份名称")
        private String provinceName;

        @ApiModelProperty(value = "城市编码")
        private String cityCode;

        @ApiModelProperty(value = "城市名称")
        private String cityName;

        @ApiModelProperty(value = "详细地址")
        private String address;

        @ApiModelProperty(value = "联系人")
        private String contactName;

        @ApiModelProperty(value = "联系电话")
        private String contactPhone;

        @ApiModelProperty(value = "场地容量")
        private Integer capacity;

        @ApiModelProperty(value = "经度")
        private java.math.BigDecimal longitude;

        @ApiModelProperty(value = "纬度")
        private java.math.BigDecimal latitude;

        @ApiModelProperty(value = "场地状态", required = true)
        private Integer venueStatus;

        @ApiModelProperty(value = "备注")
        private String remark;

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
