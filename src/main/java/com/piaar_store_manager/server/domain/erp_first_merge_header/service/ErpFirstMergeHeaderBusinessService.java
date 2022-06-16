package com.piaar_store_manager.server.domain.erp_first_merge_header.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.piaar_store_manager.server.domain.erp_first_merge_header.dto.ErpFirstMergeHeaderDto;
import com.piaar_store_manager.server.domain.erp_first_merge_header.entity.ErpFirstMergeHeaderEntity;
import com.piaar_store_manager.server.domain.user.service.UserService;
import com.piaar_store_manager.server.utils.CustomDateUtils;

@Service
@RequiredArgsConstructor
public class ErpFirstMergeHeaderBusinessService {
    private final ErpFirstMergeHeaderService erpFirstMergeHeaderService;
    private final UserService userService;

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * erp first merge header를 등록한다.
     *
     * @param headerDto : ErpFirstMergeHeaderDto
     * @see ErpFirstMergeHeaderEntity#toEntity
     * @see ErpFirstMergeHeaderService#saveAndModify
     */
    public void saveOne(ErpFirstMergeHeaderDto headerDto) {
        UUID ID = UUID.randomUUID();
        UUID USER_ID = userService.getUserId();
        ErpFirstMergeHeaderEntity headerEntity = ErpFirstMergeHeaderEntity.toEntity(headerDto);
        headerEntity
            .setId(ID)
            .setCreatedAt(CustomDateUtils.getCurrentDateTime())
            .setCreatedBy(USER_ID)
            .setUpdatedAt(CustomDateUtils.getCurrentDateTime());

        erpFirstMergeHeaderService.saveAndModify(headerEntity);
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * 저장된 erp first merge header를 조회한다.
     *
     * @return List::ErpFirstMergeHeaderDto::
     * @see ErpFirstMergeHeaderService#searchAll
     * @see ErpFirstMergeHeaderDto#toDto
     */
    public List<ErpFirstMergeHeaderDto> searchAll() {
        List<ErpFirstMergeHeaderEntity> entities = erpFirstMergeHeaderService.searchAll();
        List<ErpFirstMergeHeaderDto> dtos = entities.stream().map(r -> ErpFirstMergeHeaderDto.toDto(r)).collect(Collectors.toList());
        return dtos;
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * 저장된 erp first merge header를 변경한다.
     *
     * @param headerDto : ErpFirstMergeHeaderDto
     * @see ErpFirstMergeHeaderService#searchOne
     * @see CustomDateUtils#getCurrentDateTime
     * @see ErpFirstMergeHeaderService#saveAndModify
     */
    public void updateOne(ErpFirstMergeHeaderDto headerDto) {
        ErpFirstMergeHeaderEntity entity = erpFirstMergeHeaderService.searchOne(headerDto.getId());

        entity.getHeaderDetail().setDetails(headerDto.getHeaderDetail().getDetails());
        entity.setTitle(headerDto.getTitle()).setUpdatedAt(CustomDateUtils.getCurrentDateTime());

        erpFirstMergeHeaderService.saveAndModify(entity);
    }

    public void deleteOne(UUID id) {
        erpFirstMergeHeaderService.deleteOne(id);
    }
}
