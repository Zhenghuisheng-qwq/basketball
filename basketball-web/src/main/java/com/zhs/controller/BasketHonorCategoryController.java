package com.zhs.controller;



import com.zhs.service.IBasketHonorCategoryService;
import com.zhs.model.BasketHonorCategory;
import com.zhs.dto.BasketHonorCategoryDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/honor/category")
@Api(value = "")
@Slf4j
public class BasketHonorCategoryController {

    @Resource
    private IBasketHonorCategoryService  iBasketHonorCategoryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketHonorCategory>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketHonorCategory> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorCategory> lambda = new QueryWrapper<BasketHonorCategory>().lambda();
        //此处可以拼条件
        lambda.eq(BasketHonorCategory::getIsDelete,0);
        IPage<BasketHonorCategory> pages =  iBasketHonorCategoryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketHonorCategory> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketHonorCategory> wrapper = new QueryWrapper<BasketHonorCategory>()
                            .lambda().eq(BasketHonorCategory::getId,id).eq(BasketHonorCategory::getIsDelete,0);
        return R.ok(iBasketHonorCategoryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketHonorCategory> query = new QueryWrapper<BasketHonorCategory>().lambda().eq(BasketHonorCategory::getId, id).eq(BasketHonorCategory::getIsDelete, 0);
        BasketHonorCategory basketHonorCategory = iBasketHonorCategoryService.getOne(query);
        if(ObjectUtils.isEmpty(basketHonorCategory)) throw new ServiceException("该数据不存在或者已经被删除");
        basketHonorCategory.setIsDelete(1);
        iBasketHonorCategoryService.updateById(basketHonorCategory);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketHonorCategoryDto basketHonorCategoryDto){
        if (CollectionUtils.isEmpty(basketHonorCategoryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketHonorCategory> list = new ArrayList<>();
        basketHonorCategoryDto.getIdList().stream().forEach(id ->{
        BasketHonorCategory basketHonorCategory = iBasketHonorCategoryService.getById(id);
            if (ObjectUtils.isEmpty(basketHonorCategory)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketHonorCategory.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketHonorCategory.setIsDelete(1);
            list.add(basketHonorCategory);
        });
        iBasketHonorCategoryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketHonorCategoryDto basketHonorCategoryDto){
        return iBasketHonorCategoryService.addOrUpdate(basketHonorCategoryDto);
    }

}
