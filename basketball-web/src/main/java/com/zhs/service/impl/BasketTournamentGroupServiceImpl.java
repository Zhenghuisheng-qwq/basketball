package com.zhs.service.impl;


import com.zhs.model.BasketTournamentGroup;
import com.zhs.dao.BasketTournamentGroupDao;
import com.zhs.service.IBasketTournamentGroupService;
import com.zhs.dto.BasketTournamentGroupDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketTournamentGroupServiceImpl extends ServiceImpl< BasketTournamentGroupDao, BasketTournamentGroup> implements IBasketTournamentGroupService {

    @Resource
    private BasketTournamentGroupDao basketTournamentGroupDao;

    @Override
    public R addOrUpdate(BasketTournamentGroupDto basketTournamentGroupDto) {
        if (BeanUtil.isEmpty(basketTournamentGroupDto)) throw new ServiceException("数据不能为空");
        BasketTournamentGroup basketTournamentGroup = new BasketTournamentGroup();
        BeanUtil.copyProperties(basketTournamentGroupDto,basketTournamentGroup);
        if (basketTournamentGroupDto.getId() == null){
            basketTournamentGroupDao.insert(basketTournamentGroup);
            return R.ok("数据插入成功");
        }else {
            basketTournamentGroupDao.updateById(basketTournamentGroup);
            return R.ok("数据更新成功");
        }
    }
}