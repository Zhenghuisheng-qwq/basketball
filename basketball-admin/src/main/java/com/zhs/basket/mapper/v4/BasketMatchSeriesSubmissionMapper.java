package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSeriesSubmission;
import com.zhs.basket.query.v4.BasketSeriesSubmissionAuditQuery;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionAuditListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchSeriesSubmissionMapper {
    BasketMatchSeriesSubmission selectById(@Param("id") Long id);
    BasketMatchSeriesSubmission selectByIdForUpdate(@Param("id") Long id);
    List<BasketMatchSeriesSubmission> selectVersionsBySeriesId(@Param("matchSeriesId") Long matchSeriesId);
    List<BasketSeriesSubmissionAuditListVo> selectAuditVoList(BasketSeriesSubmissionAuditQuery query);
    int insert(BasketMatchSeriesSubmission entity);
    int updateById(BasketMatchSeriesSubmission entity);
    int deleteTombstone(@Param("matchSeriesId") Long matchSeriesId, @Param("versionNo") Integer versionNo);
    int softDelete(@Param("id") Long id);
}
