package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerMyEventsMapper;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.v4.V4MyEventResp;
import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4PlayerMyEventsServiceImpl implements IBasketV4PlayerMyEventsService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMyEventsMapper myEventsMapper;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;

    @Override
    public List<V4MyEventResp> listMyEvents() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        return myEventsMapper.selectMyEvents(memberId, player.getId());
    }

    private BasketPlayerProfile requirePlayerByMemberId(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("当前账号未关联认证球员");
        }
        return profile;
    }
}
