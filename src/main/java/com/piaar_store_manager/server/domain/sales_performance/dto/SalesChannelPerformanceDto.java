package com.piaar_store_manager.server.domain.sales_performance.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesChannelPerformanceProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SalesChannelPerformanceDto {
    private String salesChannel;
    private Integer orderPayAmount;
    private Integer salesPayAmount;
    private Integer orderRegistration;
    private Integer salesRegistration;
    private Integer orderUnit;
    private Integer salesUnit;

    public static SalesChannelPerformanceDto toDto(SalesChannelPerformanceProjection proj) {
        SalesChannelPerformanceDto dto = SalesChannelPerformanceDto.builder()
            .salesChannel(proj.getSalesChannel())
            .orderPayAmount(proj.getOrderPayAmount())
            .salesPayAmount(proj.getSalesPayAmount())
            .orderRegistration(proj.getOrderRegistration())
            .salesRegistration(proj.getSalesRegistration())
            .orderUnit(proj.getOrderUnit())
            .salesUnit(proj.getSalesUnit())
            .build();

        return dto;
    }

    @Getter
    @ToString
    @Accessors(chain = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayAmount {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime datetime;
        private List<SalesChannelPerformanceDto> performance;

        public static PayAmount toDto(SalesChannelPerformanceProjection.PayAmount proj) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            List<SalesChannelPerformanceDto> salesChannelPerformanceDtos = proj.getPerformance().stream().map(r -> SalesChannelPerformanceDto.toDto(r)).collect(Collectors.toList());

            PayAmount dto = PayAmount.builder()
                .datetime(proj.getDatetime() != null ? LocalDateTime.parse(proj.getDatetime(), formatter) : null)
                .performance(salesChannelPerformanceDtos)
                .build();

            return dto;
        }
    }

    @Getter
    @ToString
    @Accessors(chain = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationAndUnit {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime datetime;
        private List<SalesChannelPerformanceDto> performance;

        public static RegistrationAndUnit toDto(SalesChannelPerformanceProjection.RegistrationAndUnit proj) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            List<SalesChannelPerformanceDto> salesChannelPerformanceDtos = proj.getPerformance().stream().map(r -> SalesChannelPerformanceDto.toDto(r)).collect(Collectors.toList());

            RegistrationAndUnit dto = RegistrationAndUnit.builder()
                .datetime(proj.getDatetime() != null ? LocalDateTime.parse(proj.getDatetime(), formatter) : null)
                .performance(salesChannelPerformanceDtos)
                .build();

            return dto;
        }
    }

    @Getter
    @ToString
    @Accessors(chain = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesPayAmount {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime datetime;
        private List<SalesChannelPerformanceDto> performance;

        public static SalesPayAmount toDto(SalesChannelPerformanceProjection.SalesPayAmount proj) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            List<SalesChannelPerformanceDto> salesChannelPerformanceDtos = proj.getPerformance().stream().map(r -> SalesChannelPerformanceDto.toDto(r)).collect(Collectors.toList());

            SalesPayAmount dto = SalesPayAmount.builder()
                .datetime(proj.getDatetime() != null ? LocalDateTime.parse(proj.getDatetime(), formatter) : null)
                .performance(salesChannelPerformanceDtos)
                .build();

            return dto;
        }
    }
}
