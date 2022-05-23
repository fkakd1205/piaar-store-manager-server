package com.piaar_store_manager.server.domain.order_confirm.dto;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.Data;

@Data
public class OrdererDto {
    private String name;
    private String receiverName;
    private String address;
    private String phone;
    private int orderUnit;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryLimitDate;
}
