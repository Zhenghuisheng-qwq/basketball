package com.zhs.dao;


import com.zhs.model.BasketNotice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 公告内容表 的dao
 *
 * @author 
 * @since 2026-07-18
 */
@Mapper
@Repository
public interface BasketNoticeDao extends BaseMapper<BasketNotice> {

}
