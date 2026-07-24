package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertMaterial;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerCertMaterialMapper {
    List<BasketPlayerCertMaterial> selectByApplicationId(Long applicationId);
    int countByFileIdForCert(@Param("fileId") Long fileId);
}
