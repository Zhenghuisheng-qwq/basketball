package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerMyEventsMapper;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyAvailableEventItemResp;
import com.zhs.response.v4.V4MyEventResp;
import com.zhs.response.v4.V4PersonalAvailableDivisionResp;
import com.zhs.service.v4.IBasketV4PersonalRegistrationService;
import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import com.zhs.service.v4.IBasketV4PlayerRegistrationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BasketV4PlayerMyEventsServiceImpl implements IBasketV4PlayerMyEventsService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMyEventsMapper myEventsMapper;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private IBasketV4PlayerRegistrationService playerRegistrationService;
    @Resource
    private IBasketV4PersonalRegistrationService personalRegistrationService;

    @Override
    public List<V4MyEventResp> listMyEvents() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        return myEventsMapper.selectMyEvents(memberId, player.getId());
    }

    @Override
    public IPage<V4MyEventResp> pageMyEvents(Integer currentPage, Integer pageSize) {
        return toPage(listMyEvents(), currentPage, pageSize);
    }

    @Override
    public IPage<V4MyAvailableEventItemResp> pageAvailableEvents(Integer currentPage, Integer pageSize) {
        List<V4MyAvailableEventItemResp> merged = new ArrayList<>();
        List<V4PersonalAvailableDivisionResp> personalRows = personalRegistrationService.listAvailableDivisions();
        if (personalRows != null) {
            for (V4PersonalAvailableDivisionResp row : personalRows) {
                V4MyAvailableEventItemResp item = new V4MyAvailableEventItemResp();
                item.setSource("PERSONAL");
                item.setPersonalItem(row);
                merged.add(item);
            }
        }
        List<V4AvailableDivisionResp> teamRows = playerRegistrationService.listAvailableDivisions();
        if (teamRows != null) {
            for (V4AvailableDivisionResp row : teamRows) {
                V4MyAvailableEventItemResp item = new V4MyAvailableEventItemResp();
                item.setSource("TEAM");
                item.setTeamItem(row);
                merged.add(item);
            }
        }
        return toPage(merged, currentPage, pageSize);
    }

    private <T> IPage<T> toPage(List<T> items, Integer currentPage, Integer pageSize) {
        long page = currentPage == null || currentPage < 1 ? 1L : currentPage;
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 50L);
        List<T> source = items == null ? Collections.emptyList() : items;
        long total = source.size();
        if (total == 0) {
            Page<T> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        long maxPage = Math.max(1L, (total + size - 1) / size);
        if (page > maxPage) {
            page = maxPage;
        }
        int from = (int) ((page - 1) * size);
        int to = (int) Math.min(from + size, total);
        Page<T> resp = new Page<>(page, size, total);
        resp.setRecords(from >= to ? Collections.emptyList() : source.subList(from, to));
        return resp;
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