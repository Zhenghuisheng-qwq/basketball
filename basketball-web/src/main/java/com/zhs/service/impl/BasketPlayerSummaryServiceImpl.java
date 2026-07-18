package com.zhs.service.impl;


import com.zhs.model.BasketPlayerSummary;
import com.zhs.dao.BasketPlayerSummaryDao;
import com.zhs.service.IBasketPlayerSummaryService;
import com.zhs.dto.BasketPlayerSummaryDto;


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
public class BasketPlayerSummaryServiceImpl extends ServiceImpl< BasketPlayerSummaryDao, BasketPlayerSummary> implements IBasketPlayerSummaryService {

    @Resource
    private BasketPlayerSummaryDao basketPlayerSummaryDao;

    @Override
    public R addOrUpdate(BasketPlayerSummaryDto basketPlayerSummaryDto) {
        if (BeanUtil.isEmpty(basketPlayerSummaryDto)) throw new ServiceException("数据不能为空");
        BasketPlayerSummary basketPlayerSummary = new BasketPlayerSummary();
        BeanUtil.copyProperties(basketPlayerSummaryDto,basketPlayerSummary);
        if (basketPlayerSummaryDto.getId() == null){
            basketPlayerSummaryDao.insert(basketPlayerSummary);
            return R.ok("数据插入成功");
        }else {
            basketPlayerSummaryDao.updateById(basketPlayerSummary);
            return R.ok("数据更新成功");
        }
    }
}