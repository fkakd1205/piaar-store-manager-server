package com.piaar_store_manager.server.domain.erp_release_complete_header.service;

import java.util.List;

import com.piaar_store_manager.server.domain.erp_release_complete_header.entity.ErpReleaseCompleteHeaderEntity;
import com.piaar_store_manager.server.domain.erp_release_complete_header.repository.ErpReleaseCompleteHeaderRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ErpReleaseCompleteHeaderService {
    private final ErpReleaseCompleteHeaderRepository erpReleaseCompleteHeaderRepository;

    /**
     * <b>DB Insert Or Update Related Method</b>
     * <p>
     * 피아르 엑셀 헤더를 저장 or 수정한다.
     * 
     * @param entity : ErpReleaseCompleteHeaderEntity
     * @see ErpReleaseCompleteHeaderRepository#save
     */
    public void saveAndModify(ErpReleaseCompleteHeaderEntity entity) {
        erpReleaseCompleteHeaderRepository.save(entity);
    }

     /**
     * <b>DB Select Related Method</b>
     * <p>
     * erp release complete header을 조회한다.
     *
     * @return List::ErpReleaseCompleteHeaderEntity::
     * @see ErpReleaseCompleteHeaderRepository#findAll
     */
    public List<ErpReleaseCompleteHeaderEntity> findAll() {
        return erpReleaseCompleteHeaderRepository.findAll();
    }
}
