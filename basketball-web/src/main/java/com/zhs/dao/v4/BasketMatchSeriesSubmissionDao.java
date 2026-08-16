package com.zhs.dao.v4;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.v4.BasketMatchSeriesSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketMatchSeriesSubmissionDao extends BaseMapper<BasketMatchSeriesSubmission> {

    @Select("select * from basket_match_series_submission where id = #{id} and is_delete = 0 for update")
    BasketMatchSeriesSubmission selectByIdForUpdate(Long id);
}
