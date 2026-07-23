package com.zhs.controller;



import com.zhs.service.IBasketMemberAgreementRecordService;
import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.dto.BasketMemberAgreementRecordDto;

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
@RequestMapping("/basket/member/agreement/record")
@Api(value = "")
@Slf4j
public class BasketMemberAgreementRecordController {

    @Resource
    private IBasketMemberAgreementRecordService  iBasketMemberAgreementRecordService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMemberAgreementRecord>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMemberAgreementRecord> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMemberAgreementRecord> lambda = new QueryWrapper<BasketMemberAgreementRecord>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMemberAgreementRecord::getIsDelete,0);
        IPage<BasketMemberAgreementRecord> pages =  iBasketMemberAgreementRecordService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMemberAgreementRecord> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMemberAgreementRecord> wrapper = new QueryWrapper<BasketMemberAgreementRecord>()
                            .lambda().eq(BasketMemberAgreementRecord::getId,id).eq(BasketMemberAgreementRecord::getIsDelete,0);
        return R.ok(iBasketMemberAgreementRecordService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMemberAgreementRecord> query = new QueryWrapper<BasketMemberAgreementRecord>().lambda().eq(BasketMemberAgreementRecord::getId, id).eq(BasketMemberAgreementRecord::getIsDelete, 0);
        BasketMemberAgreementRecord basketMemberAgreementRecord = iBasketMemberAgreementRecordService.getOne(query);
        if(ObjectUtils.isEmpty(basketMemberAgreementRecord)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMemberAgreementRecord.setIsDelete(1);
        iBasketMemberAgreementRecordService.updateById(basketMemberAgreementRecord);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMemberAgreementRecordDto basketMemberAgreementRecordDto){
        if (CollectionUtils.isEmpty(basketMemberAgreementRecordDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMemberAgreementRecord> list = new ArrayList<>();
        basketMemberAgreementRecordDto.getIdList().stream().forEach(id ->{
        BasketMemberAgreementRecord basketMemberAgreementRecord = iBasketMemberAgreementRecordService.getById(id);
            if (ObjectUtils.isEmpty(basketMemberAgreementRecord)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMemberAgreementRecord.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMemberAgreementRecord.setIsDelete(1);
            list.add(basketMemberAgreementRecord);
        });
        iBasketMemberAgreementRecordService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMemberAgreementRecordDto basketMemberAgreementRecordDto){
        return iBasketMemberAgreementRecordService.addOrUpdate(basketMemberAgreementRecordDto);
    }

}
