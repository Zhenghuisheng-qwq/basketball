package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.dto.v4.BasketDivisionCreateReq;
import com.zhs.basket.dto.v4.BasketSeasonCreateReq;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.service.v4.IBasketV4DivisionService;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.v4.DivisionSafeRecoverySupport;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.support.v4.V4PublicReadCacheEvictor;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 非生产数据的运行时冒烟。事务回滚，不保留测试赛季，也不改 Season 150 / Division 183。
 */
@SpringJUnitConfig(DivisionSafeRecoverySmokeTest.SmokeConfig.class)
@Transactional
class DivisionSafeRecoverySmokeTest {

    private static final long PROD_SEASON_ID = 150L;
    private static final long PROD_DIVISION_ID = 183L;

    @Autowired
    private IBasketV4SeasonService seasonService;
    @Autowired
    private IBasketV4DivisionService divisionService;
    @Autowired
    private IBasketV4FormatSnapshotService formatSnapshotService;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;
    @Autowired
    private BasketRegistrationMapper registrationMapper;

    @Test
    void emptyDivisionFinishRejectedThenDeleteSeasonAndBlockedWhenRegistrationExists() {
        BasketSeason beforeSeason = seasonMapper.selectById(PROD_SEASON_ID);
        BasketDivision beforeDivision = divisionMapper.selectById(PROD_DIVISION_ID);
        assertNotNull(beforeSeason);
        assertNotNull(beforeDivision);

        long suffix = System.currentTimeMillis();
        Long seasonId = createSeason(suffix);
        Long divisionId = createDivision(seasonId, suffix);
        assertTrue(seasonId != PROD_SEASON_ID && divisionId != PROD_DIVISION_ID);

        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        assertNotNull(snapshot);
        List<BasketDivisionStage> stages = stageMapper.selectBySnapshotId(snapshot.getId());
        List<BasketDivisionTieBreakRule> tieBreaks = tieBreakRuleMapper.selectBySnapshotId(snapshot.getId());
        assertTrue(!stages.isEmpty());

        seasonService.publish(seasonId);
        formatSnapshotService.lockSnapshot(divisionId, 1L);
        divisionService.openRegistration(divisionId);
        divisionService.closeRegistration(divisionId);

        ServiceException finishEx = assertThrows(ServiceException.class,
                () -> divisionService.finish(divisionId, "冒烟误结束"));
        assertEquals(DivisionSafeRecoverySupport.FINISH_BLOCKED_MESSAGE, finishEx.getMessage());
        assertEquals("REGISTRATION_CLOSED", divisionMapper.selectById(divisionId).getDivisionStatus());

        divisionService.delete(divisionId);
        assertNull(divisionMapper.selectById(divisionId));
        assertNull(snapshotMapper.selectById(snapshot.getId()));
        for (BasketDivisionStage stage : stages) {
            assertNull(stageMapper.selectById(stage.getId()));
        }
        for (BasketDivisionTieBreakRule rule : tieBreaks) {
            assertNull(tieBreakRuleMapper.selectByStageId(rule.getStageId()).stream()
                    .filter(item -> item.getId().equals(rule.getId()))
                    .findFirst()
                    .orElse(null));
        }

        seasonService.offline(seasonId);
        seasonService.delete(seasonId);
        assertNull(seasonMapper.selectById(seasonId));

        Long blockedSeasonId = createSeason(suffix + 1);
        Long blockedDivisionId = createDivision(blockedSeasonId, suffix + 1);
        insertRegistration(blockedSeasonId, blockedDivisionId);
        ServiceException deleteEx = assertThrows(ServiceException.class,
                () -> divisionService.delete(blockedDivisionId));
        assertEquals(DivisionSafeRecoverySupport.DELETE_BLOCKED_MESSAGE, deleteEx.getMessage());
        assertNotNull(divisionMapper.selectById(blockedDivisionId));

        BasketSeason afterSeason = seasonMapper.selectById(PROD_SEASON_ID);
        BasketDivision afterDivision = divisionMapper.selectById(PROD_DIVISION_ID);
        assertEquals(beforeSeason.getPublishStatus(), afterSeason.getPublishStatus());
        assertEquals(beforeSeason.getSeasonStatus(), afterSeason.getSeasonStatus());
        assertEquals(beforeDivision.getDivisionStatus(), afterDivision.getDivisionStatus());
    }

    private Long createSeason(long suffix) {
        BasketSeasonCreateReq req = new BasketSeasonCreateReq();
        req.setSeriesId(6L);
        req.setSeasonYear(2026);
        req.setSeasonName("SR-SMOKE-" + suffix);
        req.setDisplayTitle("Safe Recovery Smoke " + suffix);
        req.setSeasonStartTime(Timestamp.valueOf("2026-11-01 00:00:00"));
        req.setSeasonEndTime(Timestamp.valueOf("2026-11-30 00:00:00"));
        return seasonService.create(req);
    }

    private Long createDivision(Long seasonId, long suffix) {
        BasketDivisionCreateReq req = new BasketDivisionCreateReq();
        req.setSeasonId(seasonId);
        req.setPlatformCode("PS_XBOX");
        req.setGameMode("FIVE_V_FIVE");
        req.setDivisionName("SR Smoke " + suffix);
        req.setRegistrationMode("TEAM");
        req.setFormatType("REGULAR_ROUND_ROBIN");
        req.setMaxTeamCount(8);
        req.setRegistrationStartTime(Timestamp.valueOf("2026-11-02 00:00:00"));
        req.setRegistrationEndTime(Timestamp.valueOf("2026-11-10 00:00:00"));
        req.setMatchStartTime(Timestamp.valueOf("2026-11-12 00:00:00"));
        req.setMatchEndTime(Timestamp.valueOf("2026-11-20 00:00:00"));
        req.setSortOrder(0);
        return divisionService.create(req);
    }

    private void insertRegistration(Long seasonId, Long divisionId) {
        Date now = new Date();
        BasketRegistration registration = new BasketRegistration();
        registration.setSeasonId(seasonId);
        registration.setDivisionId(divisionId);
        registration.setRegistrationMode("TEAM");
        registration.setRegistrationSource("TEAM");
        registration.setTeamId(1L);
        registration.setCaptainPlayerId(1L);
        registration.setCaptainMemberId(1L);
        registration.setRegistrationStatus("DRAFT");
        registration.setLineupCount(0);
        registration.setCreateTime(now);
        registration.setUpdateTime(now);
        registration.setIsDelete(0);
        registrationMapper.insert(registration);
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.zhs.basket.mapper")
    @ComponentScan(
            basePackages = {
                    "com.zhs.basket.service.impl.v4",
                    "com.zhs.basket.support.v4",
                    "com.zhs.basket.support"
            },
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {V4PublicReadCacheEvictor.class, BasketPublicCacheCleaner.class}
            )
    )
    static class SmokeConfig {
        @Bean
        public static BeanFactoryPostProcessor lazyBeans() {
            return beanFactory -> {
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    beanFactory.getBeanDefinition(name).setLazyInit(true);
                }
            };
        }

        @Bean
        public DataSource dataSource() throws Exception {
            String text = Files.readString(Path.of("src/main/resources/application-druid.yml"));
            Matcher url = Pattern.compile("url:\\s*jdbc:mysql://([^:/]+):(\\d+)/(\\w+)").matcher(text);
            Matcher user = Pattern.compile("username:\\s*(\\S+)").matcher(text);
            Matcher pwd = Pattern.compile("password:\\s*(\\S+)").matcher(text);
            if (!url.find() || !user.find() || !pwd.find()) {
                throw new IllegalStateException("无法读取后台数据源配置");
            }
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl("jdbc:mysql://" + url.group(1) + ":" + url.group(2) + "/" + url.group(3)
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
            ds.setUsername(user.group(1));
            ds.setPassword(pwd.group(1));
            return ds;
        }

        @Bean
        public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            factory.setMapperLocations(resolver.getResources("classpath*:mapper/basket/**/*Mapper.xml"));
            factory.setConfigLocation(resolver.getResource("classpath:mybatis/mybatis-config.xml"));
            return factory.getObject();
        }

        @Bean
        public DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
