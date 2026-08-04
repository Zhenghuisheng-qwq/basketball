package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import org.apache.ibatis.annotations.Param;

public interface BasketDivisionFormatSnapshotMapper {
    BasketDivisionFormatSnapshot selectById(@Param("id") Long id);

    BasketDivisionFormatSnapshot selectActiveByDivisionId(@Param("divisionId") Long divisionId);

    int selectMaxVersionByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketDivisionFormatSnapshot entity);

    int updateById(BasketDivisionFormatSnapshot entity);

    int markSuperseded(@Param("id") Long id);

    int clearActiveUniqueKey(@Param("id") Long id);
}
