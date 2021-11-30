package com.piaar_store_manager.server.model.product_category.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.piaar_store_manager.server.model.product_category.dto.ProductCategoryGetDto;

@Entity
@Data
@Accessors(chain = true)
@Table(name = "product_category")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cid")
    private Integer cid;

    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    /**
     * <b>Convert Method</b>
     * <p>
     * ProductCategoryGetDto => ProductCategoryEntity
     * 
     * @param reqDto : ProductCategoryGetDto
     * @return ProductCategoryEntity
     */
    public static ProductCategoryEntity toEntity(ProductCategoryGetDto reqDto) {
        ProductCategoryEntity entity = ProductCategoryEntity.builder()
            .id(reqDto.getId())
            .name(reqDto.getName())
            .build();

        return entity;
    }
}
