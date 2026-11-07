package com.zhs.dao.v4;

import com.zhs.model.v4.BasketSeries;
import com.zhs.model.v4.SeriesHonorIconRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * CNPA 赛事域 V4 Dao：BasketSeries
 */
@Mapper
@Repository
public interface BasketSeriesDao extends BaseMapper<BasketSeries> {

    @Select("<script>SELECT id, honor_icon_url AS honorIconUrl FROM basket_series WHERE is_delete = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<SeriesHonorIconRow> selectHonorIcons(@Param("ids") Collection<Long> ids);
}
