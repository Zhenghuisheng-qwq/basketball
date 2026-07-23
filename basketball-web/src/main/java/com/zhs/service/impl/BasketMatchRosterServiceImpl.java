package com.zhs.service.impl;


import com.zhs.model.BasketMatchRoster;
import com.zhs.dao.BasketMatchRosterDao;
import com.zhs.service.IBasketMatchRosterService;
import com.zhs.dto.BasketMatchRosterDto;


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
public class BasketMatchRosterServiceImpl extends ServiceImpl< BasketMatchRosterDao, BasketMatchRoster> implements IBasketMatchRosterService {

    @Resource
    private BasketMatchRosterDao basketMatchRosterDao;

    @Override
    public R addOrUpdate(BasketMatchRosterDto basketMatchRosterDto) {
        if (BeanUtil.isEmpty(basketMatchRosterDto)) throw new ServiceException("数据不能为空");
        BasketMatchRoster basketMatchRoster = new BasketMatchRoster();
        BeanUtil.copyProperties(basketMatchRosterDto,basketMatchRoster);
        if (basketMatchRosterDto.getId() == null){
            basketMatchRosterDao.insert(basketMatchRoster);
            return R.ok("数据插入成功");
        }else {
            basketMatchRosterDao.updateById(basketMatchRoster);
            return R.ok("数据更新成功");
        }
    }
}