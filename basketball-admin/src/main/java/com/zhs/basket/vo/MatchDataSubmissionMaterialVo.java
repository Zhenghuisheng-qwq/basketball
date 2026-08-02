package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDataSubmissionMaterialVo {
    private Long id;
    private Long fileId;
    private String materialType;
    private String materialTypeName;
    private String materialName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String uploaderName;
    private Date uploadTime;
    private Integer sortNo;
}
