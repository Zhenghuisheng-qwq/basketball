package com.zhs.controller;



import com.zhs.service.IBasketCaptainApplyService;
import com.zhs.model.BasketCaptainApply;
import com.zhs.dto.BasketCaptainApplyDto;

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
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/captain/apply")
@Api(value = "")
@Slf4j
public class BasketCaptainApplyController {

    @Resource
    private IBasketCaptainApplyService  iBasketCaptainApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketCaptainApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketCaptainApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketCaptainApply> lambda = new QueryWrapper<BasketCaptainApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketCaptainApply::getIsDelete,0);
        IPage<BasketCaptainApply> pages =  iBasketCaptainApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketCaptainApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketCaptainApply> wrapper = new QueryWrapper<BasketCaptainApply>()
                            .lambda().eq(BasketCaptainApply::getId,id).eq(BasketCaptainApply::getIsDelete,0);
        return R.ok(iBasketCaptainApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketCaptainApply> query = new QueryWrapper<BasketCaptainApply>().lambda().eq(BasketCaptainApply::getId, id).eq(BasketCaptainApply::getIsDelete, 0);
        BasketCaptainApply basketCaptainApply = iBasketCaptainApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketCaptainApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketCaptainApply.setIsDelete(1);
        iBasketCaptainApplyService.updateById(basketCaptainApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketCaptainApplyDto basketCaptainApplyDto){
        if (CollectionUtils.isEmpty(basketCaptainApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketCaptainApply> list = new ArrayList<>();
        basketCaptainApplyDto.getIdList().stream().forEach(id ->{
        BasketCaptainApply basketCaptainApply = iBasketCaptainApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketCaptainApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketCaptainApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketCaptainApply.setIsDelete(1);
            list.add(basketCaptainApply);
        });
        iBasketCaptainApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketCaptainApplyDto basketCaptainApplyDto){
        return iBasketCaptainApplyService.addOrUpdate(basketCaptainApplyDto);
    }

}
