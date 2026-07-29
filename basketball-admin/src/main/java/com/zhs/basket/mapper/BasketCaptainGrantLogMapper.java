package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketCaptainGrantLog;
import com.zhs.basket.vo.CaptainGrantLogVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketCaptainGrantLogMapper {
    int insert(BasketCaptainGrantLog log);

    List<CaptainGrantLogVo> selectByGrantId(@Param("grantId") Long grantId);
}
