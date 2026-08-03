#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate CNPA V4 Round2 admin publish backend scaffolding files."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADMIN = ROOT / "basketball-admin" / "src" / "main" / "java" / "com" / "zhs"
RES = ROOT / "basketball-admin" / "src" / "main" / "resources" / "mapper" / "basket" / "v4"


def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.lstrip("\n"), encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


# ---------- Mapper interfaces ----------
write(ADMIN / "basket/mapper/v4/BasketSeriesMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketSeries;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketSeries
 */
public interface BasketSeriesMapper {
    BasketSeries selectById(@Param("id") Long id);

    List<BasketSeries> selectList(@Param("enabled") Integer enabled);

    int insert(BasketSeries entity);

    int updateById(BasketSeries entity);

    int softDelete(@Param("id") Long id);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);
}
""")

write(ADMIN / "basket/mapper/v4/BasketSeasonMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.query.v4.BasketSeasonQuery;
import com.zhs.basket.vo.v4.BasketSeasonListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketSeason
 */
public interface BasketSeasonMapper {
    BasketSeason selectById(@Param("id") Long id);

    List<BasketSeasonListVo> selectVoList(BasketSeasonQuery query);

    BasketSeason selectBySeriesYearName(@Param("seriesId") Long seriesId,
                                        @Param("seasonYear") Integer seasonYear,
                                        @Param("seasonName") String seasonName,
                                        @Param("excludeId") Long excludeId);

    int countDivisionBySeasonId(@Param("seasonId") Long seasonId);

    int insert(BasketSeason entity);

    int updateById(BasketSeason entity);

    int softDelete(@Param("id") Long id);
}
""")

write(ADMIN / "basket/mapper/v4/BasketDivisionMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.vo.v4.BasketDivisionListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketDivision
 */
public interface BasketDivisionMapper {
    BasketDivision selectById(@Param("id") Long id);

    List<BasketDivisionListVo> selectVoList(BasketDivisionQuery query);

    List<BasketDivision> selectBySeasonId(@Param("seasonId") Long seasonId);

    BasketDivision selectBySeasonPlatform(@Param("seasonId") Long seasonId,
                                          @Param("platformCode") String platformCode,
                                          @Param("excludeId") Long excludeId);

    int countBySeasonId(@Param("seasonId") Long seasonId);

    int insert(BasketDivision entity);

    int updateById(BasketDivision entity);

    int softDelete(@Param("id") Long id);
}
""")

# ---------- Query / DTO / VO ----------
write(ADMIN / "basket/query/v4/BasketSeasonQuery.java", """
package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketSeasonQuery {
    private Long seriesId;
    private String seriesCode;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String publishStatus;
    private String seasonStatus;
}
""")

write(ADMIN / "basket/query/v4/BasketDivisionQuery.java", """
package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketDivisionQuery {
    private Long seasonId;
    private String platformCode;
    private String registrationMode;
    private String formatType;
    private String divisionStatus;
}
""")

write(ADMIN / "basket/dto/v4/BasketSeasonCreateReq.java", """
package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonCreateReq {
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private String registrationMode;
    private String formatType;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
}
""")

write(ADMIN / "basket/dto/v4/BasketSeasonUpdateReq.java", """
package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonUpdateReq {
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private String registrationMode;
    private String formatType;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
}
""")

write(ADMIN / "basket/dto/v4/BasketDivisionCreateReq.java", """
package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionCreateReq {
    private Long seasonId;
    private String platformCode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private Integer sortOrder;
}
""")

write(ADMIN / "basket/dto/v4/BasketDivisionUpdateReq.java", """
package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionUpdateReq {
    private String platformCode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private Integer sortOrder;
}
""")

write(ADMIN / "basket/dto/v4/BasketSeriesEnabledReq.java", """
package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketSeriesEnabledReq {
    /** 1 启用，0 停用 */
    private Integer enabled;
}
""")

write(ADMIN / "basket/vo/v4/BasketSeriesVo.java", """
package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeriesVo {
    private Long id;
    private String seriesCode;
    private String seriesName;
    private String seriesDesc;
    private String defaultFormatType;
    private Long iconFileId;
    private Long coverFileId;
    private Long bannerFileId;
    private Integer sortOrder;
    private Integer enabled;
    private Date createTime;
    private Date updateTime;
}
""")

write(ADMIN / "basket/vo/v4/BasketSeasonListVo.java", """
package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonListVo {
    private Long id;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private String registrationMode;
    private String formatType;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String publishStatus;
    private String seasonStatus;
    private Integer divisionCount;
    private Date createTime;
    private Date updateTime;
}
""")

write(ADMIN / "basket/vo/v4/BasketSeasonDetailVo.java", """
package com.zhs.basket.vo.v4;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasketSeasonDetailVo extends BasketSeasonListVo {
    private List<BasketDivisionListVo> divisions;
}
""")

write(ADMIN / "basket/vo/v4/BasketDivisionListVo.java", """
package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private String platformCode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String divisionStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
""")

print("DTO/VO/Query/Mapper interfaces done")
