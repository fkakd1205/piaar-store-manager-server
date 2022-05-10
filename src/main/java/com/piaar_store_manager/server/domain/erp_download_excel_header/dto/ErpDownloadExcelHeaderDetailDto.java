package com.piaar_store_manager.server.domain.erp_download_excel_header.dto;

import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErpDownloadExcelHeaderDetailDto {
    @Type(type = "jsonb")
    private List<DetailDto> details;
}
