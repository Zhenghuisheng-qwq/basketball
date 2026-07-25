package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketTournamentRosterChangeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketTournamentRosterChangeRequestDao extends BaseMapper<BasketTournamentRosterChangeRequest> {
}
