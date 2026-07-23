package com.zhs.controller;



import com.zhs.service.IBasketPlayerCertApplicationService;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.dto.BasketPlayerCertApplicationDto;

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
@RequestMapping("/basket/player/cert/application")
@Api(value = "")
@Slf4j
public class BasketPlayerCertApplicationController {

    @Resource
    private IBasketPlayerCertApplicationService  iBasketPlayerCertApplicationService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerCertApplication>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerCertApplication> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerCertApplication> lambda = new QueryWrapper<BasketPlayerCertApplication>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerCertApplication::getIsDelete,0);
        IPage<BasketPlayerCertApplication> pages =  iBasketPlayerCertApplicationService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerCertApplication> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerCertApplication> wrapper = new QueryWrapper<BasketPlayerCertApplication>()
                            .lambda().eq(BasketPlayerCertApplication::getId,id).eq(BasketPlayerCertApplication::getIsDelete,0);
        return R.ok(iBasketPlayerCertApplicationService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerCertApplication> query = new QueryWrapper<BasketPlayerCertApplication>().lambda().eq(BasketPlayerCertApplication::getId, id).eq(BasketPlayerCertApplication::getIsDelete, 0);
        BasketPlayerCertApplication basketPlayerCertApplication = iBasketPlayerCertApplicationService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerCertApplication)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerCertApplication.setIsDelete(1);
        iBasketPlayerCertApplicationService.updateById(basketPlayerCertApplication);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerCertApplicationDto basketPlayerCertApplicationDto){
        if (CollectionUtils.isEmpty(basketPlayerCertApplicationDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerCertApplication> list = new ArrayList<>();
        basketPlayerCertApplicationDto.getIdList().stream().forEach(id ->{
        BasketPlayerCertApplication basketPlayerCertApplication = iBasketPlayerCertApplicationService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerCertApplication)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerCertApplication.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerCertApplication.setIsDelete(1);
            list.add(basketPlayerCertApplication);
        });
        iBasketPlayerCertApplicationService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerCertApplicationDto basketPlayerCertApplicationDto){
        return iBasketPlayerCertApplicationService.addOrUpdate(basketPlayerCertApplicationDto);
    }

}
