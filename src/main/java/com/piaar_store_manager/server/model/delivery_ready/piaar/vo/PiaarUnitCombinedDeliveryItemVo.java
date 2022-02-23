package com.piaar_store_manager.server.model.delivery_ready.piaar.vo;

import java.util.List;

import com.piaar_store_manager.server.model.delivery_ready.piaar.dto.DeliveryReadyPiaarItemUnitCombinedDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PiaarUnitCombinedDeliveryItemVo {
    List<DeliveryReadyPiaarItemUnitCombinedDto> combinedDeliveryItems;
}
