package com.zhs.basket.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketVenue {
    private Long id;
    private String venueNo;
    private String venueName;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String address;
    private String contactName;
    private String contactPhone;
    private Integer capacity;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer venueStatus;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
