package com.zhs.service.impl;


import com.zhs.model.BasketMatchPeriodScore;
import com.zhs.dao.BasketMatchPeriodScoreDao;
import com.zhs.service.IBasketMatchPeriodScoreService;
import com.zhs.dto.BasketMatchPeriodScoreDto;


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
public class BasketMatchPeriodScoreServiceImpl extends ServiceImpl< BasketMatchPeriodScoreDao, BasketMatchPeriodScore> implements IBasketMatchPeriodScoreService {

    @Resource
    private BasketMatchPeriodScoreDao basketMatchPeriodScoreDao;

    @Override
    public R addOrUpdate(BasketMatchPeriodScoreDto basketMatchPeriodScoreDto) {
        if (BeanUtil.isEmpty(basketMatchPeriodScoreDto)) throw new ServiceException("数据不能为空");
        BasketMatchPeriodScore basketMatchPeriodScore = new BasketMatchPeriodScore();
        BeanUtil.copyProperties(basketMatchPeriodScoreDto,basketMatchPeriodScore);
        if (basketMatchPeriodScoreDto.getId() == null){
            basketMatchPeriodScoreDao.insert(basketMatchPeriodScore);
            return R.ok("数据插入成功");
        }else {
            basketMatchPeriodScoreDao.updateById(basketMatchPeriodScore);
            return R.ok("数据更新成功");
        }
    }
}