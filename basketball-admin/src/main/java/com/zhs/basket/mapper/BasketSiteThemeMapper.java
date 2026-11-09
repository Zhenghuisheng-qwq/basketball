package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketSiteTheme;
import com.zhs.basket.domain.BasketSiteThemeBinding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketSiteThemeMapper {
    List<BasketSiteTheme> selectAll();

    BasketSiteTheme selectById(@Param("id") Long id);

    BasketSiteTheme selectForUpdate(@Param("id") Long id);

    int insert(BasketSiteTheme theme);

    int updateDraft(BasketSiteTheme theme);

    int updateWorking(BasketSiteTheme theme);

    int applyPublish(BasketSiteTheme theme);

    int updateStatus(@Param("id") Long id, @Param("status") int status, @Param("versionNo") Integer versionNo, @Param("updateBy") String updateBy);

    int softDelete(@Param("id") Long id, @Param("updateBy") String updateBy);

    List<BasketSiteThemeBinding> selectLiveBindings();

    BasketSiteThemeBinding selectBinding(@Param("scopeType") String scopeType, @Param("scopeId") long scopeId);

    BasketSiteThemeBinding selectBindingForUpdate(@Param("scopeType") String scopeType, @Param("scopeId") long scopeId);

    int insertBinding(BasketSiteThemeBinding binding);

    int updateBinding(BasketSiteThemeBinding binding);

    int touchBindings(@Param("themeId") Long themeId, @Param("themeVersion") int themeVersion, @Param("updateBy") String updateBy);

    int softDeleteBindingsByTheme(@Param("themeId") Long themeId, @Param("updateBy") String updateBy);

    int softDeleteBinding(@Param("id") Long id, @Param("updateBy") String updateBy);

    String selectFileUrl(@Param("id") Long id);
}
