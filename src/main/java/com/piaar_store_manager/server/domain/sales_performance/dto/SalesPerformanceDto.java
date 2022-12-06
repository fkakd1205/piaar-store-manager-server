package com.piaar_store_manager.server.domain.sales_performance.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.piaar_store_manager.server.domain.sales_performance.proj.SalesPerformanceProjection;
import com.piaar_store_manager.server.utils.CustomDateUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

public class SalesPerformanceDto {

    @Getter
    @ToString
    @Accessors(chain = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dashboard {

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime datetime;

        private Long orderRegistration;
        private Integer orderPayAmount;

        private Long salesRegistration;
        private Integer salesPayAmount;

        public static Dashboard toDto(SalesPerformanceProjection.Dashboard proj) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            Dashboard dto = Dashboard.builder()
                .datetime(CustomDateUtils.toLocalDateStartTime(LocalDate.parse(proj.getDatetime(), formatter)))
                .orderRegistration(proj.getOrderRegistration())
                .orderPayAmount(proj.getOrderPayAmount())
                .salesRegistration(proj.getSalesRegistration())
                .salesPayAmount(proj.getSalesPayAmount())
                .build();

            return dto;
        }
    }
}
