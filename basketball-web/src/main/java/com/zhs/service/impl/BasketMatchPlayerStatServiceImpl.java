package com.zhs.service.impl;


import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.dao.BasketMatchPlayerStatDao;
import com.zhs.service.IBasketMatchPlayerStatService;
import com.zhs.dto.BasketMatchPlayerStatDto;


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
public class BasketMatchPlayerStatServiceImpl extends ServiceImpl< BasketMatchPlayerStatDao, BasketMatchPlayerStat> implements IBasketMatchPlayerStatService {

    @Resource
    private BasketMatchPlayerStatDao basketMatchPlayerStatDao;

    @Override
    public R addOrUpdate(BasketMatchPlayerStatDto basketMatchPlayerStatDto) {
        if (BeanUtil.isEmpty(basketMatchPlayerStatDto)) throw new ServiceException("数据不能为空");
        BasketMatchPlayerStat basketMatchPlayerStat = new BasketMatchPlayerStat();
        BeanUtil.copyProperties(basketMatchPlayerStatDto,basketMatchPlayerStat);
        if (basketMatchPlayerStatDto.getId() == null){
            basketMatchPlayerStatDao.insert(basketMatchPlayerStat);
            return R.ok("数据插入成功");
        }else {
            basketMatchPlayerStatDao.updateById(basketMatchPlayerStat);
            return R.ok("数据更新成功");
        }
    }
}