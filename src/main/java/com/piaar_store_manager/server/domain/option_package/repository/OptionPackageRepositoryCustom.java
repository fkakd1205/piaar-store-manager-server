package com.piaar_store_manager.server.domain.option_package.repository;

import com.piaar_store_manager.server.domain.option_package.proj.OptionPackageProjection;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionPackageRepositoryCustom {
    List<OptionPackageProjection.RelatedProductOption> qfindBatchByParentOptionId(UUID parentOptionId);
    List<OptionPackageProjection.RelatedProductOption> qfindBatchByParentOptionIds(List<UUID> parentOptionId);
}
