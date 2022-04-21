package com.piaar_store_manager.server.domain.erp_second_merge_header.dto;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DetailDto {
    private Integer cellNumber;
    private String originCellName;
    private String customCellName;
    private String matchedColumnName;
    private String mergeYn;
    private String fixedValue;
    private String splitter;
    private List<ViewDetailDto> viewDetails;
}
