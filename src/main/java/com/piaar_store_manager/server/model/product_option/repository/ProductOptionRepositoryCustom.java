package com.piaar_store_manager.server.model.product_option.repository;

import java.util.List;

import com.piaar_store_manager.server.model.product_option.proj.ProductOptionProj;

import org.springframework.stereotype.Repository;

@Repository
public interface ProductOptionRepositoryCustom {
    List<ProductOptionProj> qfindAllM2OJ();
}
