package com.zhs.service.impl;


import com.zhs.model.BasketHonorCategory;
import com.zhs.dao.BasketHonorCategoryDao;
import com.zhs.service.IBasketHonorCategoryService;
import com.zhs.dto.BasketHonorCategoryDto;


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
public class BasketHonorCategoryServiceImpl extends ServiceImpl< BasketHonorCategoryDao, BasketHonorCategory> implements IBasketHonorCategoryService {

    @Resource
    private BasketHonorCategoryDao basketHonorCategoryDao;

    @Override
    public R addOrUpdate(BasketHonorCategoryDto basketHonorCategoryDto) {
        if (BeanUtil.isEmpty(basketHonorCategoryDto)) throw new ServiceException("数据不能为空");
        BasketHonorCategory basketHonorCategory = new BasketHonorCategory();
        BeanUtil.copyProperties(basketHonorCategoryDto,basketHonorCategory);
        if (basketHonorCategoryDto.getId() == null){
            basketHonorCategoryDao.insert(basketHonorCategory);
            return R.ok("数据插入成功");
        }else {
            basketHonorCategoryDao.updateById(basketHonorCategory);
            return R.ok("数据更新成功");
        }
    }
}