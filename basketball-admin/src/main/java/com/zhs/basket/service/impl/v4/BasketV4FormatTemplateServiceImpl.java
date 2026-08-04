package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.domain.v4.BasketFormatTemplateStage;
import com.zhs.basket.domain.v4.BasketFormatTemplateTieBreakRule;
import com.zhs.basket.mapper.v4.BasketFormatTemplateMapper;
import com.zhs.basket.mapper.v4.BasketFormatTemplateStageMapper;
import com.zhs.basket.mapper.v4.BasketFormatTemplateTieBreakRuleMapper;
import com.zhs.basket.service.v4.IBasketV4FormatTemplateService;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.vo.v4.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketV4FormatTemplateServiceImpl implements IBasketV4FormatTemplateService {

    @Autowired
    private BasketFormatTemplateMapper templateMapper;
    @Autowired
    private BasketFormatTemplateStageMapper stageMapper;
    @Autowired
    private BasketFormatTemplateTieBreakRuleMapper tieBreakRuleMapper;

    @Override
    public List<BasketFormatTemplateListVo> listEnabledTemplates(Long seriesId) {
        return templateMapper.selectEnabledBySeriesId(seriesId).stream().map(t -> {
            BasketFormatTemplateListVo vo = new BasketFormatTemplateListVo();
            BeanUtils.copyBeanProp(vo, t);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public BasketFormatTemplateDetailVo getTemplateDetail(Long templateId) {
        BasketFormatTemplate template = requireEnabledTemplate(templateId);
        BasketFormatTemplateDetailVo vo = new BasketFormatTemplateDetailVo();
        BeanUtils.copyBeanProp(vo, template);
        vo.setStages(stageMapper.selectByTemplateId(templateId).stream().map(s -> {
            BasketFormatTemplateStageVo stageVo = new BasketFormatTemplateStageVo();
            BeanUtils.copyBeanProp(stageVo, s);
            V4FormatDisplaySupport.enrichTemplateStageVo(stageVo);
            return stageVo;
        }).collect(Collectors.toList()));
        vo.setTieBreakRules(tieBreakRuleMapper.selectByTemplateId(templateId).stream().map(r -> {
            BasketFormatTemplateTieBreakRuleVo ruleVo = new BasketFormatTemplateTieBreakRuleVo();
            BeanUtils.copyBeanProp(ruleVo, r);
            ruleVo.setRuleCodeName(V4FormatDisplaySupport.tieBreakRuleLabel(r.getRuleCode()));
            return ruleVo;
        }).collect(Collectors.toList()));
        return vo;
    }

    public BasketFormatTemplate requireEnabledTemplate(Long templateId) {
        if (templateId == null) {
            throw new ServiceException("formatTemplateId 不能为空");
        }
        BasketFormatTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("赛制模板不存在");
        }
        if (template.getEnabled() == null || template.getEnabled() != 1) {
            throw new ServiceException("赛制模板未启用");
        }
        return template;
    }

    public void assertTemplateBelongsToSeries(BasketFormatTemplate template, Long seriesId) {
        if (template.getSeriesId() != null && !template.getSeriesId().equals(seriesId)) {
            throw new ServiceException("赛制模板不属于当前赛事系列");
        }
    }

    public BasketFormatTemplate resolveDefaultTemplate(Long seriesId) {
        BasketFormatTemplate template = templateMapper.selectDefaultBySeriesId(seriesId);
        if (template != null) {
            return template;
        }
        List<BasketFormatTemplate> enabled = templateMapper.selectEnabledBySeriesId(seriesId);
        if (enabled.isEmpty()) {
            throw new ServiceException("当前系列没有可用的赛制模板");
        }
        return enabled.get(0);
    }
}
