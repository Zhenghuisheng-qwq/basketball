package com.zhs.basket.support;

import com.zhs.basket.domain.BasketHonorCategory;
import com.zhs.basket.enums.HonorCodeEnum;
import com.zhs.basket.mapper.BasketHonorCategoryMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 内置荣誉分类与 {@link HonorCodeEnum} 对齐。
 * 表可以是空的：启动时补齐，保存荣誉时再补一次，避免缺行导致「荣誉分类不存在」。
 */
@Component
public class BasketHonorCategoryCatalog {

    private static final Logger log = LoggerFactory.getLogger(BasketHonorCategoryCatalog.class);

    @Autowired
    private BasketHonorCategoryMapper honorCategoryMapper;

    @PostConstruct
    public void init() {
        int count = ensureBuiltIn();
        log.info("内置荣誉分类已对齐，共 {} 条", count);
    }

    public int ensureBuiltIn() {
        for (HonorCodeEnum code : HonorCodeEnum.values()) {
            upsert(code);
        }
        return HonorCodeEnum.values().length;
    }

    public BasketHonorCategory requireByCode(String honorCode) {
        if (StringUtils.isEmpty(honorCode)) {
            throw new ServiceException("荣誉分类不存在");
        }
        String code = honorCode.trim();
        HonorCodeEnum builtIn = HonorCodeEnum.find(code);
        if (builtIn != null) {
            upsert(builtIn);
        }
        BasketHonorCategory category = honorCategoryMapper.selectByCode(code);
        if (category == null) {
            throw new ServiceException("荣誉分类不存在");
        }
        return category;
    }

    private void upsert(HonorCodeEnum code) {
        BasketHonorCategory row = new BasketHonorCategory();
        row.setHonorCode(code.getCode());
        row.setHonorName(code.getName());
        row.setHonorScope(code.getScope());
        row.setRecipientType(code.getRecipientType());
        row.setDescription(code.getDescription());
        row.setDisplayOrder(code.getDisplayOrder());
        honorCategoryMapper.upsertBuiltin(row);
    }
}
