package com.zhs.controller;



import com.zhs.service.IBasketStatChangeRecordService;
import com.zhs.model.BasketStatChangeRecord;
import com.zhs.dto.BasketStatChangeRecordDto;

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
@RequestMapping("/basket/stat/change/record")
@Api(value = "")
@Slf4j
public class BasketStatChangeRecordController {

    @Resource
    private IBasketStatChangeRecordService  iBasketStatChangeRecordService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketStatChangeRecord>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketStatChangeRecord> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketStatChangeRecord> lambda = new QueryWrapper<BasketStatChangeRecord>().lambda();
        //此处可以拼条件
        lambda.eq(BasketStatChangeRecord::getIsDelete,0);
        IPage<BasketStatChangeRecord> pages =  iBasketStatChangeRecordService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketStatChangeRecord> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketStatChangeRecord> wrapper = new QueryWrapper<BasketStatChangeRecord>()
                            .lambda().eq(BasketStatChangeRecord::getId,id).eq(BasketStatChangeRecord::getIsDelete,0);
        return R.ok(iBasketStatChangeRecordService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketStatChangeRecord> query = new QueryWrapper<BasketStatChangeRecord>().lambda().eq(BasketStatChangeRecord::getId, id).eq(BasketStatChangeRecord::getIsDelete, 0);
        BasketStatChangeRecord basketStatChangeRecord = iBasketStatChangeRecordService.getOne(query);
        if(ObjectUtils.isEmpty(basketStatChangeRecord)) throw new ServiceException("该数据不存在或者已经被删除");
        basketStatChangeRecord.setIsDelete(1);
        iBasketStatChangeRecordService.updateById(basketStatChangeRecord);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketStatChangeRecordDto basketStatChangeRecordDto){
        if (CollectionUtils.isEmpty(basketStatChangeRecordDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketStatChangeRecord> list = new ArrayList<>();
        basketStatChangeRecordDto.getIdList().stream().forEach(id ->{
        BasketStatChangeRecord basketStatChangeRecord = iBasketStatChangeRecordService.getById(id);
            if (ObjectUtils.isEmpty(basketStatChangeRecord)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketStatChangeRecord.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketStatChangeRecord.setIsDelete(1);
            list.add(basketStatChangeRecord);
        });
        iBasketStatChangeRecordService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketStatChangeRecordDto basketStatChangeRecordDto){
        return iBasketStatChangeRecordService.addOrUpdate(basketStatChangeRecordDto);
    }

}
