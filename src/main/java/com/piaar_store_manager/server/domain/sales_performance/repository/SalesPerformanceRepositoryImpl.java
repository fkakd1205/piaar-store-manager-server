package com.piaar_store_manager.server.domain.sales_performance.repository;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.piaar_store_manager.server.domain.erp_order_item.entity.QErpOrderItemEntity;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection.Dashboard;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection.PayAmount;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection.RegistrationAndUnit;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection.SalesPayAmount;
import com.piaar_store_manager.server.exception.CustomInvalidDataException;
import com.piaar_store_manager.server.utils.CustomDateUtils;
import com.piaar_store_manager.server.utils.CustomFieldUtils;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class SalesPerformanceRepositoryImpl implements SalesPerformanceRepositoryCustom {
    private final JPAQueryFactory query;

    private final QErpOrderItemEntity qErpOrderItemEntity = QErpOrderItemEntity.erpOrderItemEntity;

    @Autowired
    public SalesPerformanceRepositoryImpl(
        JPAQueryFactory query
    ) {
        this.query = query;
    }

    @Override
    public List<Dashboard> qSearchDashBoardByParams(Map<String, Object> params) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if(params.get("date") == null) {
            return null;
        }
        
        List<String> dateList = Arrays.asList(params.get("date").toString().split(","));
        List<LocalDateTime> localDatetimeList = dateList.stream().map(r -> LocalDateTime.parse(r, formatter)).collect(Collectors.toList());
        List<String> dateValues =  localDatetimeList.stream().map(r -> r.plusHours(9).toLocalDate().toString()).collect(Collectors.toList());
        // List<String> dateValues =  localDatetimeList.stream().map(r -> r.plusHours(9).toLocalDate().toString()).collect(Collectors.toList());
        List<Dashboard> projs = this.getDashboardInitProjs(dateValues);

        StringPath datetime = Expressions.stringPath( "datetime");

        List<Dashboard> dashboardProjs = (List<Dashboard>) query.from(qErpOrderItemEntity)
            .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(dateFormatTemplate(dateAddHourTemplate(9), "%Y-%m-%d").in(dateValues)))
            .groupBy(dateFormatTemplate(dateAddHourTemplate(9), "%Y-%m-%d"))
            .orderBy(dateFormatTemplate(dateAddHourTemplate(9), "%Y-%m-%d").asc())
            .transform(
                GroupBy.groupBy(dateFormatTemplate(dateAddHourTemplate(9), "%Y-%m-%d"))
                // GroupBy.groupBy(dateAddHourTemplate(9))
                .list(
                    Projections.fields(
                        Dashboard.class,
                        // dateAddHourTemplate(9).as("datetime"),
                        dateFormatTemplate(dateAddHourTemplate(9), "%Y-%m-%d").as("datetime"),
                        (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
                            .then(1)
                            .otherwise(0)
                        ).sum().as("orderRegistration"),
                        (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
                        (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
                            .then(1)
                            .otherwise(0)
                        ).sum().as("salesRegistration"),
                        (new CaseBuilder()
                            .when(qErpOrderItemEntity.salesYn.eq("y"))
                            .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
                            .otherwise(0)
                        ).sum().as("salesPayAmount")
                    )
                )
            );

        this.updateDashboardProjs(projs, dashboardProjs);
        System.out.println(projs);
        return projs;
    }

    // @Override
    // public List<PayAmount> qSearchDailyPayAmountByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<PayAmount> projs = this.getPayAmountInitProjs(params);

    //     List<PayAmount> payAmountProjs = (List<PayAmount>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m-%d"))
    //         .orderBy(dateformatted("%Y-%m-%d").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m-%d"))
    //             .list(
    //                 Projections.fields(
    //                     PayAmount.class,
    //                     dateformatted("%Y-%m-%d").as("datetime"),
    //                     (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updatePayAmountProjs(projs, payAmountProjs);
    //     return projs;
    // }

    // @Override
    // public List<PayAmount> qSearchWeeklyPayAmountByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<PayAmount> projs = this.getPayAmountInitProjs(params);

    //     List<PayAmount> payAmountProjs = (List<PayAmount>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(weekformatted(7))
    //         .orderBy(weekformatted(7).asc())
    //         .transform(
    //             GroupBy.groupBy(weekformatted(7))
    //             .list(
    //                 Projections.fields(
    //                     PayAmount.class,
    //                     dateformatted("%Y-%m-%d").min().as("datetime"),
    //                     weekformatted(7).as("weekNum"),
    //                     (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updatePayAmountProjs(projs, payAmountProjs);
    //     return projs;
    // }

    // @Override
    // public List<PayAmount> qSearchMonthlyPayAmountByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<PayAmount> projs = this.getPayAmountInitProjs(params);

    //     List<PayAmount> payAmountProjs = (List<PayAmount>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m"))
    //         .orderBy(dateformatted("%Y-%m").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m"))
    //             .list(
    //                 Projections.fields(
    //                     PayAmount.class,
    //                     dateformatted("%Y-%m-%d").min().as("datetime"),
    //                     dateformatted("%Y-%m").as("monthNum"),
    //                     (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updatePayAmountProjs(projs, payAmountProjs);
    //     return projs;
    // } 

    // @Override
    // public List<RegistrationAndUnit> qSearchDailyRegistrationAndUnitByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<RegistrationAndUnit> projs = this.getRegistrationAndUnitInitProjs(params);

    //     List<RegistrationAndUnit> registrationAndUnitProjs = (List<RegistrationAndUnit>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m-%d"))
    //         .orderBy(dateformatted("%Y-%m-%d").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m-%d"))
    //             .list(
    //                 Projections.fields(
    //                     RegistrationAndUnit.class,
    //                     dateformatted("%Y-%m-%d").as("datetime"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("orderRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("orderUnit"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("salesRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("salesUnit")
    //                 )
    //             )
    //         );

    //     this.updateRegistrationAndUnitProjs(projs, registrationAndUnitProjs);
    //     return projs;
    // }

    // @Override
    // public List<RegistrationAndUnit> qSearchWeeklyRegistrationAndUnitByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<RegistrationAndUnit> projs = this.getRegistrationAndUnitInitProjs(params);

    //     List<RegistrationAndUnit> registrationAndUnitProjs = (List<RegistrationAndUnit>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(weekformatted(7))
    //         .orderBy(weekformatted(7).asc())
    //         .transform(
    //             GroupBy.groupBy(weekformatted(7))
    //             .list(
    //                 Projections.fields(
    //                     RegistrationAndUnit.class,
    //                     dateformatted("%Y-%m-%d").min().as("datetime"),
    //                     weekformatted(7).as("weekNum"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("orderRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("orderUnit"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("salesRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("salesUnit")
    //                 )
    //             )
    //         );

    //     this.updateRegistrationAndUnitProjs(projs, registrationAndUnitProjs);
    //     return projs;
    // }

    // @Override
    // public List<RegistrationAndUnit> qSearchMonthlyRegistrationAndUnitByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<RegistrationAndUnit> projs = this.getRegistrationAndUnitInitProjs(params);

    //     List<RegistrationAndUnit> registrationAndUnitProjs = (List<RegistrationAndUnit>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m"))
    //         .orderBy(dateformatted("%Y-%m").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m"))
    //             .list(
    //                 Projections.fields(
    //                     RegistrationAndUnit.class,
    //                     dateformatted("%Y-%m-%d").min().as("datetime"),
    //                     dateformatted("%Y-%m").as("monthNum"),
    //                     (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updateRegistrationAndUnitProjs(projs, registrationAndUnitProjs);
    //     return projs;
    // }

    // @Override
    // public List<SalesPayAmount> qSearchSalesPayAmountByParams(Map<String, Object> params) {
    //     // 날짜 데이터 세팅
    //     List<SalesPayAmount> projs = this.getSalesPayAmountInitProjs(params);

    //     List<SalesPayAmount> payAmountProjs = (List<SalesPayAmount>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m-%d"))
    //         .orderBy(dateformatted("%Y-%m-%d").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m-%d"))
    //             .list(
    //                 Projections.fields(
    //                     SalesPayAmount.class,
    //                     dateformatted("%Y-%m-%d").as("datetime"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updateSalesPayAmountProjs(projs, payAmountProjs);
    //     return projs;
    // }

    // @Override
    // public List<SummaryTable> qSearchSummaryTableByParams(Map<String, Object> params) {
    //     List<SummaryTable> projs = this.getSummaryTableInitProjs(params);

    //     List<SummaryTable> summaryProjs = (List<SummaryTable>) query.from(qErpOrderItemEntity)
    //         .where(qErpOrderItemEntity.channelOrderDate.isNotNull().and(withinDateRange(params)))
    //         .groupBy(dateformatted("%Y-%m-%d"))
    //         .orderBy(dateformatted("%Y-%m-%d").asc())
    //         .transform(
    //             GroupBy.groupBy(dateformatted("%Y-%m-%d"))
    //             .list(
    //                 Projections.fields(
    //                     SummaryTable.class,
    //                     dateformatted("%Y-%m-%d").as("datetime"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.cid.isNotNull())
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("orderRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.unit.isNotNull())
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("orderUnit"),
    //                     (qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge).sum()).as("orderPayAmount"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(1)
    //                         .otherwise(0)
    //                     ).sum().as("salesRegistration"),
    //                     (new CaseBuilder().when(qErpOrderItemEntity.salesYn.eq("y").and(qErpOrderItemEntity.unit.isNotNull()))
    //                         .then(qErpOrderItemEntity.unit)
    //                         .otherwise(0)
    //                     ).sum().as("salesUnit"),
    //                     (new CaseBuilder()
    //                         .when(qErpOrderItemEntity.salesYn.eq("y"))
    //                         .then(qErpOrderItemEntity.price.add(qErpOrderItemEntity.deliveryCharge))
    //                         .otherwise(0)
    //                     ).sum().as("salesPayAmount")
    //                 )
    //             )
    //         );

    //     this.updateSummaryTableProjs(projs, summaryProjs);
    //     return projs;
    // }

    // private List<PayAmount> getPayAmountInitProjs(Map<String, Object> params) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //     LocalDate startDate = null;
    //     LocalDate endDate = null;
    //     String dimension = params.get("dimension") != null ? params.get("dimension").toString() : "date";
    //     long datediff = 0;

    //     if (params.get("startDate") == null || params.get("endDate") == null) {
    //         return null;
    //     }

    //     startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).toLocalDate();
    //     endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).toLocalDate();

    //     // startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).plusHours(9).toLocalDate();
    //     // endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).plusHours(9).toLocalDate();

    //     List<PayAmount> projs = new ArrayList<>();
    //     try {
    //         datediff = CustomDateUtils.getDateDiff(startDate, endDate);

    //         for (long i = 0; i <= datediff; i++) {
    //             LocalDate datetime = startDate.plusDays(i);
    //             if(i != 0) {
    //                 if (dimension.equals("week")) {
    //                     datetime = CustomDateUtils.getFirstDateOfWeek(startDate, i);
    //                 } else if (dimension.equals("month")) {
    //                     datetime = CustomDateUtils.getFirstDateOfMonth(startDate, i);
    //                 }
    //             }

    //             // 검색날짜가 범위에 벗어난다면 for문 탈출
    //             if(datetime.isAfter(endDate)) {
    //                 break;
    //             }

    //             PayAmount proj = PayAmount.builder()
    //                     .datetime(datetime.toString())
    //                     .orderPayAmount(0)
    //                     .salesPayAmount(0)
    //                     .build();
    //             projs.add(proj);
    //         }
    //     } catch (ParseException e) {
    //         throw new CustomInvalidDataException("조회기간 데이터가 올바르지 않습니다.");
    //     }

    //     return projs;
    // }

    // private void updatePayAmountProjs(List<PayAmount> initProjs, List<PayAmount> payAmountProjs) {
    //     initProjs.forEach(r -> {
    //         payAmountProjs.forEach(r2 -> {
    //             if(r.getDatetime().equals(r2.getDatetime())) {
    //                 r.setOrderPayAmount(r2.getOrderPayAmount())
    //                  .setSalesPayAmount(r2.getSalesPayAmount());
    //             }
    //         });
    //     });
    // }

    // private List<RegistrationAndUnit> getRegistrationAndUnitInitProjs(Map<String, Object> params) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //     LocalDate startDate = null;
    //     LocalDate endDate = null;
    //     String dimension = params.get("dimension") != null ? params.get("dimension").toString() : "date";
    //     long datediff = 0;

    //     if (params.get("startDate") == null || params.get("endDate") == null) {
    //         return null;
    //     }

    //     startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).toLocalDate();
    //     endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).toLocalDate();

    //     // startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).plusHours(9).toLocalDate();
    //     // endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).plusHours(9).toLocalDate();

    //     List<RegistrationAndUnit> projs = new ArrayList<>();
    //     try {
    //         datediff = CustomDateUtils.getDateDiff(startDate, endDate);

    //         for(long i = 0; i <= datediff; i++) {
    //             LocalDate datetime = startDate.plusDays(i);
    //             if(i != 0) {
    //                 if (dimension.equals("week")) {
    //                     datetime = CustomDateUtils.getFirstDateOfWeek(startDate, i);
    //                 } else if (dimension.equals("month")) {
    //                     datetime = CustomDateUtils.getFirstDateOfMonth(startDate, i);
    //                 }
    //             }

    //             // 검색날짜가 범위에 벗어난다면 for문 탈출
    //             if(datetime.isAfter(endDate)) {
    //                 break;
    //             }

    //             RegistrationAndUnit proj = RegistrationAndUnit.builder()
    //                     .datetime(datetime.toString())
    //                     .orderRegistration(0)
    //                     .orderUnit(0)
    //                     .salesRegistration(0)
    //                     .salesUnit(0)
    //                     .build();
    //             projs.add(proj);
    //         }
    //     } catch (ParseException e) {
    //         throw new CustomInvalidDataException("조회기간 데이터가 올바르지 않습니다.");
    //     }

    //     return projs;
    // }

    // private void updateRegistrationAndUnitProjs(List<RegistrationAndUnit> initProjs, List<RegistrationAndUnit> registrationAndUnitProjs) {
    //     initProjs.forEach(r -> {
    //         registrationAndUnitProjs.forEach(r2 -> {
    //             if(r.getDatetime().equals(r2.getDatetime())) {
    //                 r.setOrderRegistration(r2.getOrderRegistration())
    //                  .setOrderUnit(r2.getOrderUnit())
    //                  .setSalesRegistration(r2.getSalesRegistration())
    //                  .setSalesUnit(r2.getSalesUnit());
    //             }
    //         });
    //     });
    // }

    private List<Dashboard> getDashboardInitProjs(List<String> dateValues) {
        List<Dashboard> projs = new ArrayList<>();
        for (int i = 0; i < dateValues.size(); i++) {
            Dashboard proj = Dashboard.builder()
                    .datetime(dateValues.get(i))
                    .orderRegistration(0)
                    .orderPayAmount(0)
                    .salesRegistration(0)
                    .salesPayAmount(0)
                    .build();

            projs.add(proj);
        }

        return projs;
    }

    private void updateDashboardProjs(List<Dashboard> initProjs, List<Dashboard> dashboardProjs) {
        initProjs.forEach(r -> {
            dashboardProjs.forEach(r2 -> {
                if(r.getDatetime().equals(r2.getDatetime())) {
                    r.setOrderRegistration(r2.getOrderRegistration())
                     .setOrderPayAmount(r2.getOrderPayAmount())
                     .setSalesRegistration(r2.getSalesRegistration())
                     .setSalesPayAmount(r2.getSalesPayAmount());
                }
            });
        });
    }

    // private List<SalesPayAmount> getSalesPayAmountInitProjs(Map<String, Object> params) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //     LocalDate startDate = null;
    //     LocalDate endDate = null;
    //     long datediff = 0;

    //     if (params.get("startDate") == null || params.get("endDate") == null) {
    //         return null;
    //     }

    //     startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).toLocalDate();
    //     endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).toLocalDate();

    //     // startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).plusHours(9).toLocalDate();
    //     // endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).plusHours(9).toLocalDate();

    //     List<SalesPayAmount> projs = new ArrayList<>();
    //     try {
    //         datediff = CustomDateUtils.getDateDiff(startDate, endDate);

    //         for (long i = 0; i < datediff; i++) {
    //             LocalDate datetime = startDate.plusDays(i);

    //             // 검색날짜가 범위에 벗어난다면 for문 탈출
    //             if(datetime.isAfter(endDate)) {
    //                 break;
    //             }

    //             SalesPayAmount proj = SalesPayAmount.builder()
    //                     .datetime(datetime.toString())
    //                     .salesPayAmount(0)
    //                     .build();
    //             projs.add(proj);
    //         }
    //     } catch (ParseException e) {
    //         throw new CustomInvalidDataException("조회기간 데이터가 올바르지 않습니다.");
    //     }

    //     return projs;
    // }

    // private void updateSalesPayAmountProjs(List<SalesPayAmount> initProjs, List<SalesPayAmount> payAmountProjs) {
    //     initProjs.forEach(r -> {
    //         payAmountProjs.forEach(r2 -> {
    //             if(r.getDatetime().equals(r2.getDatetime())) {
    //                  r.setSalesPayAmount(r2.getSalesPayAmount());
    //             }
    //         });
    //     });
    // }

    // private List<SummaryTable> getSummaryTableInitProjs(Map<String, Object> params) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //     LocalDate startDate = null;
    //     LocalDate endDate = null;
    //     long datediff = 0;

    //     if (params.get("startDate") == null || params.get("endDate") == null) {
    //         return null;
    //     }

    //     startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).toLocalDate();
    //     endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).toLocalDate();

    //     // startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).plusHours(9).toLocalDate();
    //     // endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).plusHours(9).toLocalDate();

    //     List<SummaryTable> projs = new ArrayList<>();
    //     try {
    //         datediff = CustomDateUtils.getDateDiff(startDate, endDate);

    //         for (long i = 0; i < datediff; i++) {
    //             LocalDate datetime = startDate.plusDays(i);

    //             // 검색날짜가 범위에 벗어난다면 for문 탈출
    //             if(datetime.isAfter(endDate)) {
    //                 break;
    //             }

    //             SummaryTable proj = SummaryTable.builder()
    //                     .datetime(datetime.toString())
    //                     .orderRegistration(0)
    //                     .orderUnit(0)
    //                     .orderPayAmount(0)
    //                     .salesRegistration(0)
    //                     .salesUnit(0)
    //                     .salesPayAmount(0)
    //                     .build();
    //             projs.add(proj);
    //         }
    //     } catch (ParseException e) {
    //         throw new CustomInvalidDataException("조회기간 데이터가 올바르지 않습니다.");
    //     }

    //     return projs;
    // }

    // private void updateSummaryTableProjs(List<SummaryTable> initProjs, List<SummaryTable> summaryProjs) {
    //     initProjs.forEach(r -> {
    //         summaryProjs.forEach(r2 -> {
    //             if(r.getDatetime().equals(r2.getDatetime())) {
    //                 r.setOrderRegistration(r2.getOrderRegistration())
    //                  .setOrderUnit(r2.getOrderUnit())
    //                  .setOrderPayAmount(r2.getOrderPayAmount())
    //                  .setSalesRegistration(r2.getSalesRegistration())
    //                  .setSalesUnit(r2.getSalesUnit())
    //                  .setSalesPayAmount(r2.getSalesPayAmount());
    //             }
    //         });
    //     });
    // }
    
    // private BooleanExpression withinDateRange(Map<String, Object> params) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //     LocalDateTime startDate = null;
    //     LocalDateTime endDate = null;

    //     if (params.get("startDate") == null || params.get("endDate") == null) {
    //         return null;
    //     }

    //     startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter);
    //     endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter);

    //     // startDate = LocalDateTime.parse(params.get("startDate").toString(), formatter).plusHours(9);
    //     // endDate = LocalDateTime.parse(params.get("endDate").toString(), formatter).plusHours(9);

    //     if (startDate.isAfter(endDate)) {
    //         throw new CustomInvalidDataException("조회기간을 정확히 선택해 주세요.");
    //     }

    //     return qErpOrderItemEntity.channelOrderDate.between(startDate, endDate);
    // }

    private DateTemplate<String> dateFormatTemplate(DateTemplate<String> channelOrderDate, String format) {
        DateTemplate<String> formattedDate = Expressions.dateTemplate(
            String.class,
            "DATE_FORMAT({0}, {1})",
            channelOrderDate,
            ConstantImpl.create(format)
        );

        return formattedDate;
    }

    // private DateTemplate<LocalDateTime> dateAddHourTemplate(int hour) {
    //     DateTemplate<LocalDateTime> addDate = Expressions.dateTemplate(
    //         LocalDateTime.class,
    //         "DATE_ADD({0}, INTERVAL {1s} HOUR)",
    //         qErpOrderItemEntity.channelOrderDate, 
    //         ConstantImpl.create(hour)
    //     );

    //     return addDate;
    // }

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

    // private DateTemplate<String> weekformatted(Integer mode) {
    //     DateTemplate<String> formattedDate = Expressions.dateTemplate(
    //         String.class,
    //         "WEEK({0}, {1})",
    //         qErpOrderItemEntity.channelOrderDate,
    //         ConstantImpl.create(mode)
    //     );

    //     return formattedDate;   
    // }
}
