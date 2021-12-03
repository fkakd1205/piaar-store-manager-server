package com.piaar_store_manager.server.model.delivery_ready.naver.proj;

import com.piaar_store_manager.server.model.delivery_ready.naver.dto.DeliveryReadyNaverItemViewResDto;
import com.piaar_store_manager.server.model.delivery_ready.naver.dto.DeliveryReadyNaverItemDto;
import com.piaar_store_manager.server.model.delivery_ready.naver.entity.DeliveryReadyNaverItemEntity;

public interface DeliveryReadyNaverItemViewProj {
    DeliveryReadyNaverItemEntity getDeliveryReadyItem();
    String getProdManufacturingCode();
    String getOptionDefaultName();
    String getOptionManagementName();
    Integer getOptionStockUnit();
    String getOptionMemo();
    String getProdManagementName();
    String getOptionNosUniqueCode();

    /**
     * <b>Convert Method</b>
     * <p>
     * DeliveryReadyNaverItemViewProj => DeliveryReadyNaverItemViewResDto
     * 
     * @param itemViewProj : DeliveryReadyNaverItemViewProj
     * @return DeliveryReadyNaverItemViewResDto
     */
    public static DeliveryReadyNaverItemViewResDto toResDto(DeliveryReadyNaverItemViewProj itemViewProj) {
        DeliveryReadyNaverItemViewResDto dto = new DeliveryReadyNaverItemViewResDto();

        dto.setDeliveryReadyItem(DeliveryReadyNaverItemDto.toDto(itemViewProj.getDeliveryReadyItem()))
            .setProdManufacturingCode(itemViewProj.getProdManufacturingCode())
            .setProdManagementName(itemViewProj.getProdManagementName())
            .setOptionDefaultName(itemViewProj.getOptionDefaultName())
            .setOptionManagementName(itemViewProj.getOptionManagementName())
            .setOptionStockUnit(itemViewProj.getOptionStockUnit())
            .setOptionMemo(itemViewProj.getOptionMemo())
            .setOptionNosUniqueCode(itemViewProj.getOptionNosUniqueCode());

        return dto;
    }
}