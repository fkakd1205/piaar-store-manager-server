package com.piaar_store_manager.server.domain.product_category.repository;

import com.piaar_store_manager.server.domain.product_category.entity.ProductCategoryEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Integer> {
    Optional<ProductCategoryEntity> findById(UUID id);
}
