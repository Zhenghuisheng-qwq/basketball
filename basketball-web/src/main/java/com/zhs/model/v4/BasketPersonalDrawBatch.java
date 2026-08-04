package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("basket_personal_draw_batch")
public class BasketPersonalDrawBatch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long divisionId;
    private String batchCode;
    private String batchName;
    private Integer batchVersion;
    private String batchStatus;
    private String activeUniqueKey;
    private String resultSource;
    private String resultRemark;
    private String unassignedPolicy;
    private String unassignedRemark;
    private Long confirmedBy;
    private Date confirmedTime;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
