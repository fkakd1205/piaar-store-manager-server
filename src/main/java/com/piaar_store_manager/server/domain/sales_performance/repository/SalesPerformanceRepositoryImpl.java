package com.piaar_store_manager.server.domain.sales_performance.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.piaar_store_manager.server.domain.erp_order_item.entity.QErpOrderItemEntity;
import com.piaar_store_manager.server.domain.product.entity.QProductEntity;
import com.piaar_store_manager.server.domain.product_category.entity.QProductCategoryEntity;
import com.piaar_store_manager.server.domain.product_option.entity.QProductOptionEntity;
import com.piaar_store_manager.server.domain.sales_performance.filter.ChannelPerformanceSearchFilter;
import com.piaar_store_manager.server.domain.sales_performance.filter.DashboardPerformanceSearchFilter;
import com.piaar_store_manager.server.domain.sales_performance.filter.ProductPerformanceSearchFilter;
import com.piaar_store_manager.server.domain.sales_performance.filter.SalesPerformanceSearchFilter;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesCategoryPerformanceProjection;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesChannelPerformanceProjection;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesProductPerformanceProjection;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesProductPerformanceProjection.BestOptionPerformance;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesProductPerformanceProjection.BestProductPerformance;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesProductPerformanceProjection.Performance;
import com.piaar_store_manager.server.exception.CustomInvalidDataException;
import com.piaar_store_manager.server.utils.CustomDateUtils;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class SalesPerformanceRepositoryImpl implements SalesPerformanceRepositoryCustom {
    private final JPAQueryFactory query;

    private final QErpOrderItemEntity qErpOrderItemEntity = QErpOrderItemEntity.erpOrderItemEntity;
    private final QProductEntity qProductEntity = QProductEntity.productEntity;
    private final QProductOptionEntity qProductOptionEntity = QProductOptionEntity.productOptionEntity;
    private final QProductCategoryEntity qProductCategoryEntity = QProductCategoryEntity.productCategoryEntity;

    @Autowired
    public SalesPerformanceRepositoryImpl(
        JPAQueryFactory query
    ) {
        this.query = query;
    }

    @Override
    public List<SalesPerformanceProjection> qSearchDashBoardByParams(DashboardPerformanceSearchFilter filter) {
        List<LocalDateTime> localDatetimeList = filter.getSearchDate();
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        List<String> dateValues =  localDatetimeList.stream().map(r -> CustomDateUtils.changeUtcDateTime(r, utcHourDifference).toLocalDate().toString()).collect(Collectors.toList());
        
        // 날짜별 데이터 초기화
        List<SalesPerformanceProjection> projs = this.getDashboardInitProjs(dateValues);

        StringPath datetime = Expressions.stringPath("datetime");

        List<SalesPerformanceProjection> dashboardProjs = query
                .select(
                        Projections.fields(SalesPerformanceProjection.class,
                                dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").as(datetime),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull()).then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as("salesPayAmount")))
                .from(qErpOrderItemEntity)
                .where(dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").in(dateValues))
                .groupBy(datetime)
                .orderBy(datetime.asc())
                .fetch();

        // 실행 결과로 projs를 세팅
        this.updateDashboardProjs(projs, dashboardProjs);
        return projs;
    }

    @Override
    public List<SalesPerformanceProjection> qSearchSalesPerformance(SalesPerformanceSearchFilter filter) {
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;

        // 날짜별 데이터 초기화
        List<SalesPerformanceProjection> projs = this.getSalesPerformanceInitProjs(filter);

        StringPath datetime = Expressions.stringPath("datetime");

        List<SalesPerformanceProjection> performanceProjs = query
                .select(
                        Projections.fields(SalesPerformanceProjection.class,
                                dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").as(datetime),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                                        .then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as("salesPayAmount")))
                .from(qErpOrderItemEntity)
                .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate())))
                .groupBy(dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d"))
                .orderBy(dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").asc())
                .fetch();

        // 실행 결과로 projs를 세팅
        this.updateSalesPerformanceProjs(projs, performanceProjs);
        return projs;
    }

    @Override
    public List<SalesChannelPerformanceProjection.Performance> qSearchSalesPerformanceByChannel(ChannelPerformanceSearchFilter filter) {
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;

        // 날짜별 채널데이터 초기화
        List<SalesChannelPerformanceProjection.Performance> projs = this.getSalesChannelPerformanceInitProjs(filter);

        StringPath salesChannel = Expressions.stringPath("salesChannel");
        StringPath datetime = Expressions.stringPath("datetime");

        List<SalesChannelPerformanceProjection> performanceProjs = query
                .select(
                        Projections.fields(SalesChannelPerformanceProjection.class,
                                dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").as(datetime),
                                qErpOrderItemEntity.salesChannel.coalesce("").as(salesChannel),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                                        .then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as("salesPayAmount")))
                .from(qErpOrderItemEntity)
                .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate()))
                        .and(eqSearchCondition(filter)))
                .groupBy(salesChannel, datetime)
                .orderBy(datetime.asc())
                .fetch();

        // 실행 결과로 projs를 세팅
        this.updateSalesChannelPerformanceProjs(projs, performanceProjs);
        return projs;
    }

    @Override
    public List<SalesCategoryPerformanceProjection.Performance> qSearchSalesPerformanceByCategory(SalesPerformanceSearchFilter filter, List<String> categoryName) {
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        
        // 날짜별 카테고리 데이터 초기화
        List<SalesCategoryPerformanceProjection.Performance> projs = this.getSalesCategoryPerformanceInitProjs(filter, categoryName);

        StringPath productCategoryName = Expressions.stringPath("productCategoryName");
        StringPath datetime = Expressions.stringPath("datetime");

        List<SalesCategoryPerformanceProjection> performanceProjs = query
            .select(
                Projections.fields(SalesCategoryPerformanceProjection.class,
                dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").as(datetime),
                (ExpressionUtils.as(JPAExpressions.select(qProductCategoryEntity.name)
                    .from(qProductOptionEntity)
                    .join(qProductEntity).on(qProductEntity.id.eq(qProductOptionEntity.productId))
                    .join(qProductCategoryEntity).on(qProductCategoryEntity.cid.eq(qProductEntity.productCategoryCid))
                    .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code))
                    ,
                    productCategoryName
                )),
                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                    .then(1)
                    .otherwise(0)
                ).sum().as("orderRegistration"),
                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                    .then(qErpOrderItemEntity.unit)
                    .otherwise(0)
                ).sum().as("orderUnit"),
                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(1)
                    .otherwise(0)
                ).sum().as("salesRegistration"),
                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(qErpOrderItemEntity.unit)
                    .otherwise(0)
                ).sum().as("salesUnit"),
                (new CaseBuilder()
                    .when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                    .otherwise(0)
                ).sum().as("salesPayAmount")
            ))
            .from(qErpOrderItemEntity)
            .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate())))
            .groupBy(productCategoryName, datetime)
            .orderBy(datetime.asc())
            .fetch();

        // 실행 결과로 projs를 세팅
        this.updateSalesCategoryPerformanceProjs(projs, performanceProjs);
        return projs;
    }

    @Override
    public List<SalesCategoryPerformanceProjection.ProductPerformance> qSearchSalesProductPerformanceByCategory(SalesPerformanceSearchFilter filter, List<String> categoryName) {
        // 카테고리별 데이터 초기화
        List<SalesCategoryPerformanceProjection.ProductPerformance> projs = this.getSalesCategoryAndProductPerformanceInitProjs(categoryName);

        StringPath productCategoryName = Expressions.stringPath("productCategoryName");
        StringPath productName = Expressions.stringPath("productName");

        List<SalesCategoryPerformanceProjection> performanceProjs = query
            .select(
                Projections.fields(SalesCategoryPerformanceProjection.class,
                (ExpressionUtils.as(JPAExpressions.select(qProductCategoryEntity.name)
                    .from(qProductOptionEntity)
                    .join(qProductEntity).on(qProductEntity.id.eq(qProductOptionEntity.productId))
                    .join(qProductCategoryEntity).on(qProductCategoryEntity.cid.eq(qProductEntity.productCategoryCid))
                    .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code))
                    ,
                    productCategoryName
                )),
                (ExpressionUtils.as(JPAExpressions.select(qProductEntity.defaultName)
                    .from(qProductOptionEntity)
                    .join(qProductEntity).on(qProductEntity.id.eq(qProductOptionEntity.productId))
                    .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code))
                    ,
                    productName
                )),
                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                    .then(1)
                    .otherwise(0)
                ).sum().as("orderRegistration"),
                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                    .then(qErpOrderItemEntity.unit)
                    .otherwise(0)
                ).sum().as("orderUnit"),
                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(1)
                    .otherwise(0)
                ).sum().as("salesRegistration"),
                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(qErpOrderItemEntity.unit)
                    .otherwise(0)
                ).sum().as("salesUnit"),
                (new CaseBuilder()
                    .when(qErpOrderItemEntity.salesYn.eq("y"))
                    .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                    .otherwise(0)
                ).sum().as("salesPayAmount")
            ))
            .from(qErpOrderItemEntity)
            .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate())))
            .groupBy(productName)
            .orderBy(productCategoryName.asc())
            .fetch();

        // 실행 결과로 projs를 세팅
        this.updateSalesCategoryAndProductPerformanceProjs(projs, performanceProjs);
        return projs;
    }

    @Override
    public List<Performance> qSearchSalesPerformanceByProductOption(ProductPerformanceSearchFilter filter) {
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;

        // 날짜별 채널데이터 초기화
        List<SalesProductPerformanceProjection.Performance> projs = this.getSalesProductOptionPerformanceInitProjs(filter);

        StringPath datetime = Expressions.stringPath("datetime");
        StringPath productDefaultName = Expressions.stringPath("productDefaultName");
        StringPath optionDefaultName = Expressions.stringPath("optionDefaultName");
        StringPath productCode = Expressions.stringPath("productCode");
        StringPath optionCode = Expressions.stringPath("optionCode");

        List<SalesProductPerformanceProjection> performanceProjs = query
                .select(
                        Projections.fields(SalesProductPerformanceProjection.class,
                                dateFormatTemplate(dateAddHourTemplate(utcHourDifference), "%Y-%m-%d").as(datetime),
                                qProductEntity.defaultName.as(productDefaultName),
                                qProductEntity.code.as(productCode),
                                qProductOptionEntity.code.as(optionCode),
                                qProductOptionEntity.defaultName.as(optionDefaultName),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                                        .then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as("salesPayAmount")))
                .from(qErpOrderItemEntity)
                .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate()))
                        .and(eqSearchCondition(filter)))
                .leftJoin(qProductOptionEntity).on(qProductOptionEntity.code.eq(qErpOrderItemEntity.optionCode))
                .leftJoin(qProductEntity).on(qProductEntity.cid.eq(qProductOptionEntity.productCid))
                .groupBy(optionCode, datetime)
                .fetch();

        // 실행 결과로 projs를 세팅
        this.updateSalesProductOptionPerformanceProjs(projs, performanceProjs);
        return projs;
    }

    @Override
    public List<BestProductPerformance> qSearchBestProductPerformance(ProductPerformanceSearchFilter filter) {
        StringPath productDefaultName = Expressions.stringPath("productDefaultName");
        NumberPath<Integer> salesPayAmount = Expressions.numberPath(Integer.class, "salesPayAmount");

        List<BestProductPerformance> performanceProjs = query
                .select(
                        Projections.fields(BestProductPerformance.class,
                                (ExpressionUtils.as(JPAExpressions.select(qProductEntity.defaultName)
                                        .from(qProductOptionEntity)
                                        .join(qProductEntity).on(qProductEntity.id.eq(qProductOptionEntity.productId))
                                        .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code)),
                                        productDefaultName)),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                                        .then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as(salesPayAmount)))
                .from(qErpOrderItemEntity)
                .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate()))
                        // .and(eqSearchCondition(filter))
                )
                .groupBy(productDefaultName)
                .orderBy(salesPayAmount.desc())
                .fetch();

        return performanceProjs;
    }

    @Override
    public List<BestOptionPerformance> qSearchBestProductOptionPerformance(ProductPerformanceSearchFilter filter) {
        StringPath productDefaultName = Expressions.stringPath("productDefaultName");
        StringPath optionDefaultName = Expressions.stringPath("optionDefaultName");
        NumberPath<Integer> salesPayAmount = Expressions.numberPath(Integer.class, "salesPayAmount");

        List<BestOptionPerformance> performanceProjs = query
                .select(
                        Projections.fields(BestOptionPerformance.class,
                                (ExpressionUtils.as(JPAExpressions.select(qProductEntity.defaultName)
                                        .from(qProductOptionEntity)
                                        .join(qProductEntity).on(qProductEntity.id.eq(qProductOptionEntity.productId))
                                        .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code)),
                                        productDefaultName)),
                                (ExpressionUtils.as(JPAExpressions.select(qProductOptionEntity.defaultName)
                                        .from(qProductOptionEntity)
                                        .where(qErpOrderItemEntity.optionCode.eq(qProductOptionEntity.code)),
                                        optionDefaultName)),
                                (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                                        .then(1)
                                        .otherwise(0)).sum().as("orderRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("orderUnit"),
                                (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum())
                                        .as("orderPayAmount"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(1)
                                        .otherwise(0)).sum().as("salesRegistration"),
                                (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.unit)
                                        .otherwise(0)).sum().as("salesUnit"),
                                (new CaseBuilder()
                                        .when(qErpOrderItemEntity.salesYn.eq("y"))
                                        .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                                        .otherwise(0)).sum().as(salesPayAmount)))
                .from(qErpOrderItemEntity)
                .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(filter.getStartDate(), filter.getEndDate()))
                        // .and(eqSearchCondition(filter))
                )
                .groupBy(optionDefaultName)
                .orderBy(salesPayAmount.desc())
                .fetch();

        return performanceProjs;
    }

    /*
     * date range 설정
     */
    private BooleanExpression withinDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        if (startDate.isAfter(endDate)) {
            throw new CustomInvalidDataException("조회기간을 정확히 선택해 주세요.");
        }

        return qErpOrderItemEntity.channelOrderDate.between(startDate, endDate);
    }

    /*
     * search option code
     */
    private BooleanExpression eqSearchCondition(ChannelPerformanceSearchFilter filter) {
        List<String> searchOptionCode = filter.getOptionCodes();
        if (searchOptionCode == null || searchOptionCode.size() == 0) {
            return null;
        }
        return qErpOrderItemEntity.optionCode.isNotEmpty().and(qErpOrderItemEntity.optionCode.in(searchOptionCode));
    }

    /*
     * search option code
     */
    private BooleanExpression eqSearchCondition(ProductPerformanceSearchFilter filter) {
        List<String> searchOptionCode = filter.getOptionCodes();
        if (searchOptionCode == null || searchOptionCode.size() == 0) {
            return null;
        }
        return qErpOrderItemEntity.optionCode.isNotEmpty().and(qErpOrderItemEntity.optionCode.in(searchOptionCode));
    }

    /*
     * date format setting
     */
    private DateTemplate<String> dateFormatTemplate(DateTemplate<String> channelOrderDate, String format) {
        DateTemplate<String> formattedDate = Expressions.dateTemplate(
            String.class,
            "DATE_FORMAT({0}, {1})",
            channelOrderDate,
            ConstantImpl.create(format)
        );

        return formattedDate;
    }

    /*
     * hour setting
     */
    private DateTemplate<String> dateAddHourTemplate(int hour) {
        LocalTime addTime = LocalTime.of(hour, 0);

        DateTemplate<String> addDate = Expressions.dateTemplate(
            String.class,
            "ADDTIME({0}, {1})",
            qErpOrderItemEntity.channelOrderDate, 
            ConstantImpl.create(addTime)
        );

        return addDate;
    }

    /*
     * dashboard projs 세팅
     */
    private List<SalesPerformanceProjection> getDashboardInitProjs(List<String> dateValues) {
        List<SalesPerformanceProjection> projs = new ArrayList<>();
        for (int i = 0; i < dateValues.size(); i++) {
            SalesPerformanceProjection proj = SalesPerformanceProjection.builder()
                    .datetime(dateValues.get(i))
                    .orderRegistration(0)
                    .orderUnit(0)
                    .orderPayAmount(0)
                    .salesRegistration(0)
                    .salesUnit(0)
                    .salesPayAmount(0)
                    .build();

            projs.add(proj);
        }

        return projs;
    }

    private void updateDashboardProjs(List<SalesPerformanceProjection> initProjs, List<SalesPerformanceProjection> dashboardProjs) {
        initProjs.forEach(r -> {
            dashboardProjs.forEach(r2 -> {
                if(r.getDatetime().equals(r2.getDatetime())) {
                    r.setOrderRegistration(r2.getOrderRegistration())
                     .setOrderUnit(r2.getOrderUnit())
                     .setOrderPayAmount(r2.getOrderPayAmount())
                     .setSalesRegistration(r2.getSalesRegistration())
                     .setSalesUnit(r2.getSalesUnit())
                     .setSalesPayAmount(r2.getSalesPayAmount());
                }
            });
        });
    }

    /*
     * sales performance projs 세팅
     */
    private List<SalesPerformanceProjection> getSalesPerformanceInitProjs(SalesPerformanceSearchFilter filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        
        if (startDate == null || endDate == null) {
            return null;
        }

        startDate = CustomDateUtils.changeUtcDateTime(startDate, utcHourDifference);
        endDate = CustomDateUtils.changeUtcDateTime(endDate, utcHourDifference);

        int dateDiff = (int) Duration.between(startDate, endDate).toDays();

        List<SalesPerformanceProjection> projs = new ArrayList<>();
        for (int i = 0; i <= dateDiff; i++) {
            LocalDateTime datetime = startDate.plusDays(i);

            // 검색날짜가 범위에 벗어난다면 for문 탈출
            if (datetime.isAfter(endDate)) {
                break;
            }

            SalesPerformanceProjection proj = SalesPerformanceProjection.builder()
                    .datetime(datetime.toLocalDate().toString())
                    .orderRegistration(0)
                    .orderUnit(0)
                    .orderPayAmount(0)
                    .salesRegistration(0)
                    .salesUnit(0)
                    .salesPayAmount(0)
                    .build();
            projs.add(proj);
        }

        return projs;
    }

    private void updateSalesPerformanceProjs(List<SalesPerformanceProjection> initProjs, List<SalesPerformanceProjection> performanceProjs) {
        initProjs.forEach(r -> {
            performanceProjs.forEach(r2 -> {
                if(r.getDatetime().equals(r2.getDatetime())) {
                    r.setOrderRegistration(r2.getOrderRegistration())
                     .setOrderUnit(r2.getOrderUnit())
                     .setOrderPayAmount(r2.getOrderPayAmount())
                     .setSalesRegistration(r2.getSalesRegistration())
                     .setSalesUnit(r2.getSalesUnit())
                     .setSalesPayAmount(r2.getSalesPayAmount());
                }
            });
        });
    }

    private List<SalesChannelPerformanceProjection.Performance> getSalesChannelPerformanceInitProjs(ChannelPerformanceSearchFilter filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        int dateDiff = (int) Duration.between(filter.getStartDate(), filter.getEndDate()).toDays();

        if (startDate == null || endDate == null) {
            return null;
        }
        

        startDate = CustomDateUtils.changeUtcDateTime(filter.getStartDate(), utcHourDifference);
        endDate = CustomDateUtils.changeUtcDateTime(filter.getEndDate(), utcHourDifference);

        List<SalesChannelPerformanceProjection.Performance> projs = new ArrayList<>();
        for (int i = 0; i <= dateDiff; i++) {
            LocalDateTime datetime = startDate.plusDays(i);

            // 검색날짜가 범위에 벗어난다면 for문 탈출
            if (datetime.isAfter(endDate)) {
                break;
            }

            SalesChannelPerformanceProjection.Performance proj = SalesChannelPerformanceProjection.Performance.builder()
                .datetime(datetime.toLocalDate().toString())
                .build();
            projs.add(proj);
        }
        return projs;
    }

    private List<SalesChannelPerformanceProjection.Performance> updateSalesChannelPerformanceProjs(List<SalesChannelPerformanceProjection.Performance> initProjs, List<SalesChannelPerformanceProjection> performanceProjs) {
        List<SalesChannelPerformanceProjection.Performance> projs = new ArrayList<>();

        initProjs.forEach(r -> {
            List<SalesChannelPerformanceProjection> salesChannelProjs = new ArrayList<>();
            performanceProjs.forEach(r2 -> {
                if(r.getDatetime().equals(r2.getDatetime())) {
                    String channelName = r2.getSalesChannel().isBlank() ? "미지정" : r2.getSalesChannel();

                    SalesChannelPerformanceProjection salesChannelProj = SalesChannelPerformanceProjection.builder()
                        .datetime(r2.getDatetime())
                        .salesChannel(channelName)
                        .orderRegistration(r2.getOrderRegistration())
                        .orderUnit(r2.getOrderUnit())
                        .orderPayAmount(r2.getOrderPayAmount())
                        .salesRegistration(r2.getSalesRegistration())
                        .salesUnit(r2.getSalesUnit())
                        .salesPayAmount(r2.getSalesPayAmount())
                        .build();

                    salesChannelProjs.add(salesChannelProj);
                }
                r.setPerformance(salesChannelProjs);
            });
        });
        projs.addAll(initProjs);
        return projs;
    }

    /*
     * sales category performance projs 세팅
     */
    private List<SalesCategoryPerformanceProjection.Performance> getSalesCategoryPerformanceInitProjs(SalesPerformanceSearchFilter filter, List<String> categoryName) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        int dateDiff = (int) Duration.between(startDate, endDate).toDays();
        categoryName.add("미지정");
        
        if (startDate == null || endDate == null) {
            return null;
        }

        startDate = CustomDateUtils.changeUtcDateTime(startDate, utcHourDifference);
        endDate = CustomDateUtils.changeUtcDateTime(endDate, utcHourDifference);

        List<SalesCategoryPerformanceProjection> performances = categoryName.stream().map(r -> {
            SalesCategoryPerformanceProjection performance = SalesCategoryPerformanceProjection.builder()
                .productCategoryName(r)
                .orderRegistration(0)
                .orderUnit(0)
                .orderPayAmount(0)
                .salesRegistration(0)
                .salesUnit(0)
                .salesPayAmount(0)
                .build();
            return performance;
        }).collect(Collectors.toList());

        List<SalesCategoryPerformanceProjection.Performance> projs = new ArrayList<>();
        for (int i = 0; i <= dateDiff; i++) {
            LocalDateTime datetime = startDate.plusDays(i);

            // 검색날짜가 범위에 벗어난다면 for문 탈출
            if (datetime.isAfter(endDate)) {
                break;
            }

            SalesCategoryPerformanceProjection.Performance proj = SalesCategoryPerformanceProjection.Performance.builder()
                .datetime(datetime.toLocalDate().toString())
                .performance(performances)
                .build();
            projs.add(proj);
        }

        return projs;
    }

    private void updateSalesCategoryPerformanceProjs(List<SalesCategoryPerformanceProjection.Performance> initProjs, List<SalesCategoryPerformanceProjection> performanceProjs) {
        initProjs.forEach(r -> {
            performanceProjs.forEach(r2 -> {
                if (r.getDatetime().equals(r2.getDatetime())) {
                    List<SalesCategoryPerformanceProjection> updatedPerform = r.getPerformance().stream().map(r3 -> {
                        String categoryName = (r3.getProductCategoryName() == null || r3.getProductCategoryName().isBlank()) ? "미지정" : r3.getProductCategoryName();
                        if(categoryName.equals(r2.getProductCategoryName())) {
                            SalesCategoryPerformanceProjection p = SalesCategoryPerformanceProjection.builder()
                                    .productCategoryName(categoryName)
                                    .orderRegistration(r2.getOrderRegistration())
                                    .orderUnit(r2.getOrderUnit())
                                    .orderPayAmount(r2.getOrderPayAmount())
                                    .salesRegistration(r2.getSalesRegistration())
                                    .salesUnit(r2.getSalesUnit())
                                    .salesPayAmount(r2.getSalesPayAmount())
                                    .build();
                                return p;
                        }else {
                            return r3;
                        }
                    }).collect(Collectors.toList());
    
                    r.setPerformance(updatedPerform);
                }
            });
        });
    }

    /*
     * sales category and product performance projs 세팅
     */
    private List<SalesCategoryPerformanceProjection.ProductPerformance> getSalesCategoryAndProductPerformanceInitProjs(List<String> categoryName) {
        List<SalesCategoryPerformanceProjection.ProductPerformance> projs = new ArrayList<>();
        
        for (int i = 0; i < categoryName.size(); i++) {
            SalesCategoryPerformanceProjection.ProductPerformance proj = SalesCategoryPerformanceProjection.ProductPerformance.builder()
                .productCategoryName(categoryName.get(i))
                .build();
            projs.add(proj);
        }
        return projs;
    }

    private void updateSalesCategoryAndProductPerformanceProjs(List<SalesCategoryPerformanceProjection.ProductPerformance> initProjs, List<SalesCategoryPerformanceProjection> performanceProjs) {
        initProjs.forEach(r -> {
            List<SalesCategoryPerformanceProjection> performances = new ArrayList<>();
            performanceProjs.forEach(r2 -> {
                if(r.getProductCategoryName().equals(r2.getProductCategoryName())) {
                    SalesCategoryPerformanceProjection performance = SalesCategoryPerformanceProjection.builder()
                        .productCategoryName(r2.getProductCategoryName())
                        .productName(r2.getProductName())
                        .orderRegistration(r2.getOrderRegistration())
                        .orderUnit(r2.getOrderUnit())
                        .orderPayAmount(r2.getOrderPayAmount())
                        .salesRegistration(r2.getSalesRegistration())
                        .salesUnit(r2.getSalesUnit())
                        .salesPayAmount(r2.getSalesPayAmount())
                        .build();

                    performances.add(performance);
                }
            });
            r.setPerformance(performances);
        });
    }

    private List<SalesProductPerformanceProjection.Performance> getSalesProductOptionPerformanceInitProjs(ProductPerformanceSearchFilter filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        int utcHourDifference = filter.getUtcHourDifference() != null ? filter.getUtcHourDifference() : 0;
        int dateDiff = (int) Duration.between(filter.getStartDate(), filter.getEndDate()).toDays();

        if (startDate == null || endDate == null) {
            return null;
        }
        
        startDate = CustomDateUtils.changeUtcDateTime(filter.getStartDate(), utcHourDifference);
        endDate = CustomDateUtils.changeUtcDateTime(filter.getEndDate(), utcHourDifference);

        List<SalesProductPerformanceProjection.Performance> projs = new ArrayList<>();
        for (int i = 0; i <= dateDiff; i++) {
            LocalDateTime datetime = startDate.plusDays(i);

            // 검색날짜가 범위에 벗어난다면 for문 탈출
            if (datetime.isAfter(endDate)) {
                break;
            }

            SalesProductPerformanceProjection.Performance proj = SalesProductPerformanceProjection.Performance.builder()
                .datetime(datetime.toLocalDate().toString())
                .build();
            projs.add(proj);
        }
        return projs;
    }

    private List<SalesProductPerformanceProjection.Performance> updateSalesProductOptionPerformanceProjs(List<SalesProductPerformanceProjection.Performance> initProjs, List<SalesProductPerformanceProjection> performanceProjs) {
        List<SalesProductPerformanceProjection.Performance> projs = new ArrayList<>();

        initProjs.forEach(r -> {
            List<SalesProductPerformanceProjection> salesProductOptionProjs = new ArrayList<>();
            performanceProjs.forEach(r2 -> {
                if(r.getDatetime().equals(r2.getDatetime())) {
                    SalesProductPerformanceProjection salesProductOptionProj = SalesProductPerformanceProjection.builder()
                        .datetime(r2.getDatetime())
                        .productCode(r2.getProductCode())
                        .productDefaultName(r2.getProductDefaultName())
                        .optionCode(r2.getOptionCode())
                        .optionDefaultName(r2.getOptionDefaultName())
                        .orderRegistration(r2.getOrderRegistration())
                        .orderUnit(r2.getOrderUnit())
                        .orderPayAmount(r2.getOrderPayAmount())
                        .salesRegistration(r2.getSalesRegistration())
                        .salesUnit(r2.getSalesUnit())
                        .salesPayAmount(r2.getSalesPayAmount())
                        .build();

                    salesProductOptionProjs.add(salesProductOptionProj);
                }
                r.setPerformance(salesProductOptionProjs);
            });
        });
        projs.addAll(initProjs);
        return projs;
    }
}
