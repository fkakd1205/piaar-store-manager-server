package com.piaar_store_manager.server.model.product_release.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

import com.piaar_store_manager.server.model.product_release.entity.ProductReleaseEntity;

@Repository
public interface ProductReleaseCustomJdbc {
    void jdbcBulkInsert(List<ProductReleaseEntity> entities);
}
