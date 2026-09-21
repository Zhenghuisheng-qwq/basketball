package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSiteSocialLink {
    private Long id;
    private String platformCode;
    private String title;
    private String subtitle;
    private String buttonText;
    private String jumpUrl;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
