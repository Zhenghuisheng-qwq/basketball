package com.zhs.service.impl;


import com.zhs.model.BasketMatch;
import com.zhs.dao.BasketMatchDao;
import com.zhs.service.IBasketMatchService;
import com.zhs.dto.BasketMatchDto;


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
public class BasketMatchServiceImpl extends ServiceImpl< BasketMatchDao, BasketMatch> implements IBasketMatchService {

    @Resource
    private BasketMatchDao basketMatchDao;

    @Override
    public R addOrUpdate(BasketMatchDto basketMatchDto) {
        if (BeanUtil.isEmpty(basketMatchDto)) throw new ServiceException("数据不能为空");
        BasketMatch basketMatch = new BasketMatch();
        BeanUtil.copyProperties(basketMatchDto,basketMatch);
        if (basketMatchDto.getId() == null){
            basketMatchDao.insert(basketMatch);
            return R.ok("数据插入成功");
        }else {
            basketMatchDao.updateById(basketMatch);
            return R.ok("数据更新成功");
        }
    }
}