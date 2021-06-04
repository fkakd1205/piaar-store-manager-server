package com.piaar_store_manager.server.model.message;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class Message {
    private HttpStatus status;
    private String message;
    private String memo;
    private Object data;
    public Message() {
        this.status = HttpStatus.BAD_REQUEST;
        this.message = null;
        this.memo = null;
        this.data = null;
    }

    public int getStatusCode(){
        return this.status.value();
    }
}
