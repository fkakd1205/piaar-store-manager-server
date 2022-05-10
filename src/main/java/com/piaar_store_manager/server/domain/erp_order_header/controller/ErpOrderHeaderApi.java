package com.piaar_store_manager.server.domain.erp_order_header.controller;


import com.piaar_store_manager.server.annotation.RequiredLogin;
import com.piaar_store_manager.server.domain.erp_order_header.dto.ErpOrderHeaderDto;
import com.piaar_store_manager.server.domain.erp_order_header.service.ErpOrderHeaderBusinessService;
import com.piaar_store_manager.server.domain.message.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/erp-order-headers")
@RequiredLogin
public class ErpOrderHeaderApi {
    private ErpOrderHeaderBusinessService erpOrderHeaderBusinessService;

    @Autowired
    public ErpOrderHeaderApi(ErpOrderHeaderBusinessService erpOrderHeaderBusinessService) {
        this.erpOrderHeaderBusinessService = erpOrderHeaderBusinessService;
    }

    /**
     * Create one api for erp order header.
     * <p>
     * <b>POST : API URL => /api/v1/erp-order-headers</b>
     * 
     * @param headerDto : ErpOrderHeaderDto
     * @return ResponseEntity(message, HttpStatus)
     * @see ErpOrderHeaderBusinessService#saveOne
     */
    @PostMapping("")
    public ResponseEntity<?> saveOne(@RequestBody ErpOrderHeaderDto headerDto) {
        Message message = new Message();

        erpOrderHeaderBusinessService.saveOne(headerDto);
        message.setStatus(HttpStatus.OK);
        message.setMessage("success");

        return new ResponseEntity<>(message, message.getStatus());
    }

    /**
     * Search one api for erp order header.
     * <p>
     * <b>GET : API URL => /api/v1/erp-order-headers</b>
     *
     * @return ResponseEntity(message, HttpStatus)
     * @see ErpOrderHeaderBusinessService#searchOne
     */
    @GetMapping("")
    public ResponseEntity<?> searchOne() {
        Message message = new Message();

        message.setData(erpOrderHeaderBusinessService.searchOne());
        message.setStatus(HttpStatus.OK);
        message.setMessage("success");

        return new ResponseEntity<>(message, message.getStatus());
    }

    /**
     * Update one api for erp order header.
     * <p>
     * <b>PUT : API URL => /api/v1/erp-order-headers</b>
     * 
     * @param headerDto : ErpOrderHeaderDto
     * @return ResponseEntity(message, HttpStatus)
     * @see ErpOrderHeaderBusinessService#updateOne
     */
    @PutMapping("")
    public ResponseEntity<?> updateOne(@RequestBody ErpOrderHeaderDto headerDto) {
        Message message = new Message();

        erpOrderHeaderBusinessService.updateOne(headerDto);
        message.setStatus(HttpStatus.OK);
        message.setMessage("success");

        return new ResponseEntity<>(message, message.getStatus());
    }
}
