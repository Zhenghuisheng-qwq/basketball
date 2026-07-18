package com.zhs.service.impl;


import com.zhs.model.BasketHonor;
import com.zhs.dao.BasketHonorDao;
import com.zhs.service.IBasketHonorService;
import com.zhs.dto.BasketHonorDto;


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
public class BasketHonorServiceImpl extends ServiceImpl< BasketHonorDao, BasketHonor> implements IBasketHonorService {

    @Resource
    private BasketHonorDao basketHonorDao;

    @Override
    public R addOrUpdate(BasketHonorDto basketHonorDto) {
        if (BeanUtil.isEmpty(basketHonorDto)) throw new ServiceException("数据不能为空");
        BasketHonor basketHonor = new BasketHonor();
        BeanUtil.copyProperties(basketHonorDto,basketHonor);
        if (basketHonorDto.getId() == null){
            basketHonorDao.insert(basketHonor);
            return R.ok("数据插入成功");
        }else {
            basketHonorDao.updateById(basketHonor);
            return R.ok("数据更新成功");
        }
    }
}