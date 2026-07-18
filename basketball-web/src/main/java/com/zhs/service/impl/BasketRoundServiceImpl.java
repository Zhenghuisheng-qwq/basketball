package com.zhs.service.impl;


import com.zhs.model.BasketRound;
import com.zhs.dao.BasketRoundDao;
import com.zhs.service.IBasketRoundService;
import com.zhs.dto.BasketRoundDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketRoundServiceImpl extends ServiceImpl< BasketRoundDao, BasketRound> implements IBasketRoundService {

    @Resource
    private BasketRoundDao basketRoundDao;

    @Override
    public R addOrUpdate(BasketRoundDto basketRoundDto) {
        if (BeanUtil.isEmpty(basketRoundDto)) throw new ServiceException("数据不能为空");
        BasketRound basketRound = new BasketRound();
        BeanUtil.copyProperties(basketRoundDto,basketRound);
        if (basketRoundDto.getId() == null){
            basketRoundDao.insert(basketRound);
            return R.ok("数据插入成功");
        }else {
            basketRoundDao.updateById(basketRound);
            return R.ok("数据更新成功");
        }
    }
}