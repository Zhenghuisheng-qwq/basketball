package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionMaterialVo {
    private Long id;
    private Long fileId;
    private String materialType;
    private String materialName;
    private String fileUrl;
    private Integer sortNo;
}
