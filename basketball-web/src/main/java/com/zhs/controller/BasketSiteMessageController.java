package com.zhs.controller;



import com.zhs.service.IBasketSiteMessageService;
import com.zhs.model.BasketSiteMessage;
import com.zhs.dto.BasketSiteMessageDto;

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
@RequestMapping("/basket/site/message")
@Api(value = "")
@Slf4j
public class BasketSiteMessageController {

    @Resource
    private IBasketSiteMessageService  iBasketSiteMessageService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketSiteMessage>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketSiteMessage> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketSiteMessage> lambda = new QueryWrapper<BasketSiteMessage>().lambda();
        //此处可以拼条件
        lambda.eq(BasketSiteMessage::getIsDelete,0);
        IPage<BasketSiteMessage> pages =  iBasketSiteMessageService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketSiteMessage> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketSiteMessage> wrapper = new QueryWrapper<BasketSiteMessage>()
                            .lambda().eq(BasketSiteMessage::getId,id).eq(BasketSiteMessage::getIsDelete,0);
        return R.ok(iBasketSiteMessageService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketSiteMessage> query = new QueryWrapper<BasketSiteMessage>().lambda().eq(BasketSiteMessage::getId, id).eq(BasketSiteMessage::getIsDelete, 0);
        BasketSiteMessage basketSiteMessage = iBasketSiteMessageService.getOne(query);
        if(ObjectUtils.isEmpty(basketSiteMessage)) throw new ServiceException("该数据不存在或者已经被删除");
        basketSiteMessage.setIsDelete(1);
        iBasketSiteMessageService.updateById(basketSiteMessage);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketSiteMessageDto basketSiteMessageDto){
        if (CollectionUtils.isEmpty(basketSiteMessageDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketSiteMessage> list = new ArrayList<>();
        basketSiteMessageDto.getIdList().stream().forEach(id ->{
        BasketSiteMessage basketSiteMessage = iBasketSiteMessageService.getById(id);
            if (ObjectUtils.isEmpty(basketSiteMessage)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketSiteMessage.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketSiteMessage.setIsDelete(1);
            list.add(basketSiteMessage);
        });
        iBasketSiteMessageService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketSiteMessageDto basketSiteMessageDto){
        return iBasketSiteMessageService.addOrUpdate(basketSiteMessageDto);
    }

}
