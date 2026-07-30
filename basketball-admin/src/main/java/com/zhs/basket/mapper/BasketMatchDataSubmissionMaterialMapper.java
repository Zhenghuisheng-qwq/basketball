package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
import com.zhs.basket.vo.MatchDataSubmissionMaterialVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataSubmissionMaterialMapper {
    int batchInsert(@Param("list") List<BasketMatchDataSubmissionMaterial> list);

    List<BasketMatchDataSubmissionMaterial> selectBySubmissionId(Long submissionId);

    List<MatchDataSubmissionMaterialVo> selectVoBySubmissionId(Long submissionId);
}
