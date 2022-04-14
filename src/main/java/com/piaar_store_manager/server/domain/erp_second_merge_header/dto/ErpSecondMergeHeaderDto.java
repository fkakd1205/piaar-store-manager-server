package com.piaar_store_manager.server.domain.erp_second_merge_header.dto;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

import com.piaar_store_manager.server.domain.erp_second_merge_header.entity.ErpSecondMergeHeaderEntity;

@Builder
@Getter
@ToString
@Accessors(chain=true)
@AllArgsConstructor
@NoArgsConstructor
public class ErpSecondMergeHeaderDto {
    private Integer cid;

    @Setter
    private UUID id;
    private String title;
    private ErpSecondMergeHeaderDetailDto headerDetail;

    @Setter
    private LocalDateTime createdAt;

    @Setter
    private UUID createdBy;

    @Setter
    private LocalDateTime updatedAt;

    public static ErpSecondMergeHeaderDto toDto(ErpSecondMergeHeaderEntity entity) {
        if(entity == null) return null;

        ErpSecondMergeHeaderDto dto = ErpSecondMergeHeaderDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .headerDetail(entity.getHeaderDetail())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();

        return dto;
    }
}
