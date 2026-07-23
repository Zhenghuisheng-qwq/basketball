package com.zhs.controller;



import com.zhs.service.IBasketAnnouncementAttachmentService;
import com.zhs.model.BasketAnnouncementAttachment;
import com.zhs.dto.BasketAnnouncementAttachmentDto;

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
@RequestMapping("/basket/announcement/attachment")
@Api(value = "")
@Slf4j
public class BasketAnnouncementAttachmentController {

    @Resource
    private IBasketAnnouncementAttachmentService  iBasketAnnouncementAttachmentService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketAnnouncementAttachment>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketAnnouncementAttachment> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketAnnouncementAttachment> lambda = new QueryWrapper<BasketAnnouncementAttachment>().lambda();
        //此处可以拼条件
        lambda.eq(BasketAnnouncementAttachment::getIsDelete,0);
        IPage<BasketAnnouncementAttachment> pages =  iBasketAnnouncementAttachmentService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketAnnouncementAttachment> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketAnnouncementAttachment> wrapper = new QueryWrapper<BasketAnnouncementAttachment>()
                            .lambda().eq(BasketAnnouncementAttachment::getId,id).eq(BasketAnnouncementAttachment::getIsDelete,0);
        return R.ok(iBasketAnnouncementAttachmentService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketAnnouncementAttachment> query = new QueryWrapper<BasketAnnouncementAttachment>().lambda().eq(BasketAnnouncementAttachment::getId, id).eq(BasketAnnouncementAttachment::getIsDelete, 0);
        BasketAnnouncementAttachment basketAnnouncementAttachment = iBasketAnnouncementAttachmentService.getOne(query);
        if(ObjectUtils.isEmpty(basketAnnouncementAttachment)) throw new ServiceException("该数据不存在或者已经被删除");
        basketAnnouncementAttachment.setIsDelete(1);
        iBasketAnnouncementAttachmentService.updateById(basketAnnouncementAttachment);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketAnnouncementAttachmentDto basketAnnouncementAttachmentDto){
        if (CollectionUtils.isEmpty(basketAnnouncementAttachmentDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketAnnouncementAttachment> list = new ArrayList<>();
        basketAnnouncementAttachmentDto.getIdList().stream().forEach(id ->{
        BasketAnnouncementAttachment basketAnnouncementAttachment = iBasketAnnouncementAttachmentService.getById(id);
            if (ObjectUtils.isEmpty(basketAnnouncementAttachment)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketAnnouncementAttachment.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketAnnouncementAttachment.setIsDelete(1);
            list.add(basketAnnouncementAttachment);
        });
        iBasketAnnouncementAttachmentService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketAnnouncementAttachmentDto basketAnnouncementAttachmentDto){
        return iBasketAnnouncementAttachmentService.addOrUpdate(basketAnnouncementAttachmentDto);
    }

}
