package com.zhs.controller;



import com.zhs.service.IBasketLeaveApplyService;
import com.zhs.model.BasketLeaveApply;
import com.zhs.dto.BasketLeaveApplyDto;

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
@RequestMapping("/basket/leave/apply")
@Api(value = "")
@Slf4j
public class BasketLeaveApplyController {

    @Resource
    private IBasketLeaveApplyService  iBasketLeaveApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketLeaveApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketLeaveApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketLeaveApply> lambda = new QueryWrapper<BasketLeaveApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketLeaveApply::getIsDelete,0);
        IPage<BasketLeaveApply> pages =  iBasketLeaveApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketLeaveApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketLeaveApply> wrapper = new QueryWrapper<BasketLeaveApply>()
                            .lambda().eq(BasketLeaveApply::getId,id).eq(BasketLeaveApply::getIsDelete,0);
        return R.ok(iBasketLeaveApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketLeaveApply> query = new QueryWrapper<BasketLeaveApply>().lambda().eq(BasketLeaveApply::getId, id).eq(BasketLeaveApply::getIsDelete, 0);
        BasketLeaveApply basketLeaveApply = iBasketLeaveApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketLeaveApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketLeaveApply.setIsDelete(1);
        iBasketLeaveApplyService.updateById(basketLeaveApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketLeaveApplyDto basketLeaveApplyDto){
        if (CollectionUtils.isEmpty(basketLeaveApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketLeaveApply> list = new ArrayList<>();
        basketLeaveApplyDto.getIdList().stream().forEach(id ->{
        BasketLeaveApply basketLeaveApply = iBasketLeaveApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketLeaveApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketLeaveApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketLeaveApply.setIsDelete(1);
            list.add(basketLeaveApply);
        });
        iBasketLeaveApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketLeaveApplyDto basketLeaveApplyDto){
        return iBasketLeaveApplyService.addOrUpdate(basketLeaveApplyDto);
    }

}
