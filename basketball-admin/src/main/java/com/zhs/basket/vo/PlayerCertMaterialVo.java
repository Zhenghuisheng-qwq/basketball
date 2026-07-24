package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class PlayerCertMaterialVo {
    private Long materialId;
    private String materialType;
    private Long fileId;
    private String materialName;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Integer sortNo;
    private Date uploadTime;
}
