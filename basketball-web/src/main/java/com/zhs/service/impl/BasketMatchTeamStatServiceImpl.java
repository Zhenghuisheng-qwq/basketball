package com.zhs.service.impl;


import com.zhs.model.BasketMatchTeamStat;
import com.zhs.dao.BasketMatchTeamStatDao;
import com.zhs.service.IBasketMatchTeamStatService;
import com.zhs.dto.BasketMatchTeamStatDto;


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
public class BasketMatchTeamStatServiceImpl extends ServiceImpl< BasketMatchTeamStatDao, BasketMatchTeamStat> implements IBasketMatchTeamStatService {

    @Resource
    private BasketMatchTeamStatDao basketMatchTeamStatDao;

    @Override
    public R addOrUpdate(BasketMatchTeamStatDto basketMatchTeamStatDto) {
        if (BeanUtil.isEmpty(basketMatchTeamStatDto)) throw new ServiceException("数据不能为空");
        BasketMatchTeamStat basketMatchTeamStat = new BasketMatchTeamStat();
        BeanUtil.copyProperties(basketMatchTeamStatDto,basketMatchTeamStat);
        if (basketMatchTeamStatDto.getId() == null){
            basketMatchTeamStatDao.insert(basketMatchTeamStat);
            return R.ok("数据插入成功");
        }else {
            basketMatchTeamStatDao.updateById(basketMatchTeamStat);
            return R.ok("数据更新成功");
        }
    }
}