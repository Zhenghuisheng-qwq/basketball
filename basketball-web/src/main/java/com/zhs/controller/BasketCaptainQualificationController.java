package com.zhs.controller;



import com.zhs.service.IBasketCaptainQualificationService;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.dto.BasketCaptainQualificationDto;

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
@RequestMapping("/basket/captain/qualification")
@Api(value = "")
@Slf4j
public class BasketCaptainQualificationController {

    @Resource
    private IBasketCaptainQualificationService  iBasketCaptainQualificationService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketCaptainQualification>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketCaptainQualification> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketCaptainQualification> lambda = new QueryWrapper<BasketCaptainQualification>().lambda();
        //此处可以拼条件
        lambda.eq(BasketCaptainQualification::getIsDelete,0);
        IPage<BasketCaptainQualification> pages =  iBasketCaptainQualificationService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketCaptainQualification> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketCaptainQualification> wrapper = new QueryWrapper<BasketCaptainQualification>()
                            .lambda().eq(BasketCaptainQualification::getId,id).eq(BasketCaptainQualification::getIsDelete,0);
        return R.ok(iBasketCaptainQualificationService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketCaptainQualification> query = new QueryWrapper<BasketCaptainQualification>().lambda().eq(BasketCaptainQualification::getId, id).eq(BasketCaptainQualification::getIsDelete, 0);
        BasketCaptainQualification basketCaptainQualification = iBasketCaptainQualificationService.getOne(query);
        if(ObjectUtils.isEmpty(basketCaptainQualification)) throw new ServiceException("该数据不存在或者已经被删除");
        basketCaptainQualification.setIsDelete(1);
        iBasketCaptainQualificationService.updateById(basketCaptainQualification);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketCaptainQualificationDto basketCaptainQualificationDto){
        if (CollectionUtils.isEmpty(basketCaptainQualificationDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketCaptainQualification> list = new ArrayList<>();
        basketCaptainQualificationDto.getIdList().stream().forEach(id ->{
        BasketCaptainQualification basketCaptainQualification = iBasketCaptainQualificationService.getById(id);
            if (ObjectUtils.isEmpty(basketCaptainQualification)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketCaptainQualification.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketCaptainQualification.setIsDelete(1);
            list.add(basketCaptainQualification);
        });
        iBasketCaptainQualificationService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketCaptainQualificationDto basketCaptainQualificationDto){
        return iBasketCaptainQualificationService.addOrUpdate(basketCaptainQualificationDto);
    }

}
