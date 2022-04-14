package com.piaar_store_manager.server.domain.erp_second_merge_header.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.piaar_store_manager.server.domain.erp_second_merge_header.dto.ErpSecondMergeHeaderDto;
import com.piaar_store_manager.server.domain.erp_second_merge_header.entity.ErpSecondMergeHeaderEntity;
import com.piaar_store_manager.server.utils.CustomDateUtils;

@Service
@RequiredArgsConstructor
public class ErpSecondMergeHeaderBusinessService {
    private final ErpSecondMergeHeaderService erpSecondMergeHeaderService;

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * erp second merge header를 등록한다.
     *
     * @param headerDto : ErpSecondMergeHeaderDto
     * @see ErpSecondMergeHeaderEntity#toEntity
     * @see ErpSecondMergeHeaderService#saveAndModify
     */
    public void saveOne(ErpSecondMergeHeaderDto headerDto) {
        UUID ID = UUID.randomUUID();
        UUID USER_ID = UUID.randomUUID();
        headerDto
            .setId(ID)
            .setCreatedAt(CustomDateUtils.getCurrentDateTime())
            .setCreatedBy(USER_ID);
        
        ErpSecondMergeHeaderEntity headerEntity = ErpSecondMergeHeaderEntity.toEntity(headerDto);
        erpSecondMergeHeaderService.saveAndModify(headerEntity);
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * 저장된 erp second merge header를 조회한다.
     *
     * @return List::ErpSecondMergeHeaderDto::
     * @see ErpSecondMergeHeaderService#searchAll
     * @see ErpSecondMergeHeaderDto#toDto
     */
    public List<ErpSecondMergeHeaderDto> searchAll() {
        List<ErpSecondMergeHeaderEntity> entities = erpSecondMergeHeaderService.searchAll();
        List<ErpSecondMergeHeaderDto> dtos = entities.stream().map(r -> ErpSecondMergeHeaderDto.toDto(r)).collect(Collectors.toList());
        return dtos;
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * 저장된 erp second merge header를 변경한다.
     *
     * @param headerDto : ErpSecondMergeHeaderDto
     * @see ErpSecondMergeHeaderService#searchOne
     * @see CustomDateUtils#getCurrentDateTime
     * @see ErpSecondMergeHeaderService#saveAndModify
     */
    public void updateOne(ErpSecondMergeHeaderDto headerDto) {
        ErpSecondMergeHeaderEntity entity = erpSecondMergeHeaderService.searchOne(headerDto.getId());

        entity.getHeaderDetail().setDetails(headerDto.getHeaderDetail().getDetails());
        entity.setTitle(headerDto.getTitle()).setUpdatedAt(CustomDateUtils.getCurrentDateTime());

        erpSecondMergeHeaderService.saveAndModify(entity);
    }

    public void deleteOne(UUID id) {
        erpSecondMergeHeaderService.deleteOne(id);
    }
}
