package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketMatchSubmissionMaterialVo {
    private Long fileId;
    private String fileName;
    private String materialType;
    private String remark;
    private String accessUrl;
}
