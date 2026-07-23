package com.zhs.service.impl;


import com.zhs.model.BasketPlayerRank;
import com.zhs.dao.BasketPlayerRankDao;
import com.zhs.service.IBasketPlayerRankService;
import com.zhs.dto.BasketPlayerRankDto;


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
public class BasketPlayerRankServiceImpl extends ServiceImpl< BasketPlayerRankDao, BasketPlayerRank> implements IBasketPlayerRankService {

    @Resource
    private BasketPlayerRankDao basketPlayerRankDao;

    @Override
    public R addOrUpdate(BasketPlayerRankDto basketPlayerRankDto) {
        if (BeanUtil.isEmpty(basketPlayerRankDto)) throw new ServiceException("数据不能为空");
        BasketPlayerRank basketPlayerRank = new BasketPlayerRank();
        BeanUtil.copyProperties(basketPlayerRankDto,basketPlayerRank);
        if (basketPlayerRankDto.getId() == null){
            basketPlayerRankDao.insert(basketPlayerRank);
            return R.ok("数据插入成功");
        }else {
            basketPlayerRankDao.updateById(basketPlayerRank);
            return R.ok("数据更新成功");
        }
    }
}