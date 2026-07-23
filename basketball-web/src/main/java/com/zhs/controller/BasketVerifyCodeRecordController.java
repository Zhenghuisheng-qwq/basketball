package com.zhs.controller;



import com.zhs.service.IBasketVerifyCodeRecordService;
import com.zhs.model.BasketVerifyCodeRecord;
import com.zhs.dto.BasketVerifyCodeRecordDto;

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
@RequestMapping("/basket/verify/code/record")
@Api(value = "")
@Slf4j
public class BasketVerifyCodeRecordController {

    @Resource
    private IBasketVerifyCodeRecordService  iBasketVerifyCodeRecordService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketVerifyCodeRecord>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketVerifyCodeRecord> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketVerifyCodeRecord> lambda = new QueryWrapper<BasketVerifyCodeRecord>().lambda();
        //此处可以拼条件
        lambda.eq(BasketVerifyCodeRecord::getIsDelete,0);
        IPage<BasketVerifyCodeRecord> pages =  iBasketVerifyCodeRecordService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketVerifyCodeRecord> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketVerifyCodeRecord> wrapper = new QueryWrapper<BasketVerifyCodeRecord>()
                            .lambda().eq(BasketVerifyCodeRecord::getId,id).eq(BasketVerifyCodeRecord::getIsDelete,0);
        return R.ok(iBasketVerifyCodeRecordService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketVerifyCodeRecord> query = new QueryWrapper<BasketVerifyCodeRecord>().lambda().eq(BasketVerifyCodeRecord::getId, id).eq(BasketVerifyCodeRecord::getIsDelete, 0);
        BasketVerifyCodeRecord basketVerifyCodeRecord = iBasketVerifyCodeRecordService.getOne(query);
        if(ObjectUtils.isEmpty(basketVerifyCodeRecord)) throw new ServiceException("该数据不存在或者已经被删除");
        basketVerifyCodeRecord.setIsDelete(1);
        iBasketVerifyCodeRecordService.updateById(basketVerifyCodeRecord);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketVerifyCodeRecordDto basketVerifyCodeRecordDto){
        if (CollectionUtils.isEmpty(basketVerifyCodeRecordDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketVerifyCodeRecord> list = new ArrayList<>();
        basketVerifyCodeRecordDto.getIdList().stream().forEach(id ->{
        BasketVerifyCodeRecord basketVerifyCodeRecord = iBasketVerifyCodeRecordService.getById(id);
            if (ObjectUtils.isEmpty(basketVerifyCodeRecord)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketVerifyCodeRecord.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketVerifyCodeRecord.setIsDelete(1);
            list.add(basketVerifyCodeRecord);
        });
        iBasketVerifyCodeRecordService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketVerifyCodeRecordDto basketVerifyCodeRecordDto){
        return iBasketVerifyCodeRecordService.addOrUpdate(basketVerifyCodeRecordDto);
    }

}
