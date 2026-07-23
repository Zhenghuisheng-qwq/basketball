package com.zhs.service.impl;


import com.zhs.model.BasketHonorLineup;
import com.zhs.dao.BasketHonorLineupDao;
import com.zhs.service.IBasketHonorLineupService;
import com.zhs.dto.BasketHonorLineupDto;


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
public class BasketHonorLineupServiceImpl extends ServiceImpl< BasketHonorLineupDao, BasketHonorLineup> implements IBasketHonorLineupService {

    @Resource
    private BasketHonorLineupDao basketHonorLineupDao;

    @Override
    public R addOrUpdate(BasketHonorLineupDto basketHonorLineupDto) {
        if (BeanUtil.isEmpty(basketHonorLineupDto)) throw new ServiceException("数据不能为空");
        BasketHonorLineup basketHonorLineup = new BasketHonorLineup();
        BeanUtil.copyProperties(basketHonorLineupDto,basketHonorLineup);
        if (basketHonorLineupDto.getId() == null){
            basketHonorLineupDao.insert(basketHonorLineup);
            return R.ok("数据插入成功");
        }else {
            basketHonorLineupDao.updateById(basketHonorLineup);
            return R.ok("数据更新成功");
        }
    }
}