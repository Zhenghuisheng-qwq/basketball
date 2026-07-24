package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketPlayerCertMaterial {
    private Long id;
    private Long applicationId;
    private String materialType;
    private Long fileId;
    private String materialName;
    private Integer sortNo;
    private Integer materialStatus;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
