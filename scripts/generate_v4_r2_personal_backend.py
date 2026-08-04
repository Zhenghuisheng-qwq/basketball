#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate CNPA R2 personal registration backend scaffolding."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADMIN = ROOT / "basketball-admin" / "src" / "main" / "java" / "com" / "zhs"
RES = ROOT / "basketball-admin" / "src" / "main" / "resources" / "mapper" / "basket" / "v4"
WEB = ROOT / "basketball-web" / "src" / "main" / "java" / "com" / "zhs"
WEB_RES = ROOT / "basketball-web" / "src" / "main" / "resources" / "mapper" / "v4"


def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.lstrip("\n"), encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


# ---------- Mapper interfaces ----------
write(ADMIN / "basket/mapper/v4/BasketPersonalRegistrationMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalRegistration;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalRegistrationMapper {
    BasketPersonalRegistration selectById(@Param("id") Long id);

    BasketPersonalRegistration selectActiveByDivisionPlayer(@Param("divisionId") Long divisionId, @Param("playerId") Long playerId);

    List<BasketPersonalRegistrationListVo> selectVoList(BasketPersonalRegistrationQuery query);

    int countApprovedByDivisionId(@Param("divisionId") Long divisionId);

    int countActiveByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketPersonalRegistration entity);

    int updateById(BasketPersonalRegistration entity);

    int softDelete(@Param("id") Long id);
}
""")

write(ADMIN / "basket/mapper/v4/BasketPersonalRegistrationAuditMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalRegistrationAudit;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationAuditVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalRegistrationAuditMapper {
    int insert(BasketPersonalRegistrationAudit entity);

    List<BasketPersonalRegistrationAuditVo> selectVoByPersonalRegistrationId(@Param("personalRegistrationId") Long personalRegistrationId);
}
""")

write(ADMIN / "basket/mapper/v4/BasketPersonalDrawBatchMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawBatch;
import com.zhs.basket.query.v4.BasketPersonalDrawBatchQuery;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawBatchMapper {
    BasketPersonalDrawBatch selectById(@Param("id") Long id);

    BasketPersonalDrawBatch selectActiveByDivision(@Param("divisionId") Long divisionId);

    int selectNextBatchVersion(@Param("divisionId") Long divisionId);

    List<BasketPersonalDrawBatchListVo> selectVoList(BasketPersonalDrawBatchQuery query);

    int insert(BasketPersonalDrawBatch entity);

    int updateById(BasketPersonalDrawBatch entity);

    int softDelete(@Param("id") Long id);
}
""")

write(ADMIN / "basket/mapper/v4/BasketPersonalDrawTeamMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawTeam;
import com.zhs.basket.vo.v4.BasketPersonalDrawTeamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawTeamMapper {
    BasketPersonalDrawTeam selectById(@Param("id") Long id);

    List<BasketPersonalDrawTeam> selectByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawTeamVo> selectVoByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawTeamVo> selectFormationTasksByCaptainUserId(@Param("captainUserId") Long captainUserId);

    BasketPersonalDrawTeamVo selectFormationDetail(@Param("drawTeamId") Long drawTeamId);

    int insert(BasketPersonalDrawTeam entity);

    int updateById(BasketPersonalDrawTeam entity);

    int softDelete(@Param("id") Long id);

    int refreshActualMemberCount(@Param("drawTeamId") Long drawTeamId);
}
""")

write(ADMIN / "basket/mapper/v4/BasketPersonalDrawAssignmentMapper.java", """
package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawAssignment;
import com.zhs.basket.vo.v4.BasketPersonalDrawAssignmentVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawAssignmentMapper {
    BasketPersonalDrawAssignment selectById(@Param("id") Long id);

    List<BasketPersonalDrawAssignment> selectByTeamId(@Param("drawTeamId") Long drawTeamId);

    List<BasketPersonalDrawAssignment> selectByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawAssignmentVo> selectVoByTeamId(@Param("drawTeamId") Long drawTeamId);

    int insert(BasketPersonalDrawAssignment entity);

    int updateById(BasketPersonalDrawAssignment entity);

    int softDelete(@Param("id") Long id);

    int softDeleteByTeamId(@Param("drawTeamId") Long drawTeamId);
}
""")

print("done mappers")
