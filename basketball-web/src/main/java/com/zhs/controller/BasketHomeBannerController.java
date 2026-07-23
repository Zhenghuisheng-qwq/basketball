package com.zhs.controller;



import com.zhs.service.IBasketHomeBannerService;
import com.zhs.model.BasketHomeBanner;
import com.zhs.dto.BasketHomeBannerDto;

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
@RequestMapping("/basket/home/banner")
@Api(value = "")
@Slf4j
public class BasketHomeBannerController {

    @Resource
    private IBasketHomeBannerService  iBasketHomeBannerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketHomeBanner>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketHomeBanner> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketHomeBanner> lambda = new QueryWrapper<BasketHomeBanner>().lambda();
        //此处可以拼条件
        lambda.eq(BasketHomeBanner::getIsDelete,0);
        IPage<BasketHomeBanner> pages =  iBasketHomeBannerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketHomeBanner> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketHomeBanner> wrapper = new QueryWrapper<BasketHomeBanner>()
                            .lambda().eq(BasketHomeBanner::getId,id).eq(BasketHomeBanner::getIsDelete,0);
        return R.ok(iBasketHomeBannerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketHomeBanner> query = new QueryWrapper<BasketHomeBanner>().lambda().eq(BasketHomeBanner::getId, id).eq(BasketHomeBanner::getIsDelete, 0);
        BasketHomeBanner basketHomeBanner = iBasketHomeBannerService.getOne(query);
        if(ObjectUtils.isEmpty(basketHomeBanner)) throw new ServiceException("该数据不存在或者已经被删除");
        basketHomeBanner.setIsDelete(1);
        iBasketHomeBannerService.updateById(basketHomeBanner);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketHomeBannerDto basketHomeBannerDto){
        if (CollectionUtils.isEmpty(basketHomeBannerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketHomeBanner> list = new ArrayList<>();
        basketHomeBannerDto.getIdList().stream().forEach(id ->{
        BasketHomeBanner basketHomeBanner = iBasketHomeBannerService.getById(id);
            if (ObjectUtils.isEmpty(basketHomeBanner)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketHomeBanner.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketHomeBanner.setIsDelete(1);
            list.add(basketHomeBanner);
        });
        iBasketHomeBannerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketHomeBannerDto basketHomeBannerDto){
        return iBasketHomeBannerService.addOrUpdate(basketHomeBannerDto);
    }

}
