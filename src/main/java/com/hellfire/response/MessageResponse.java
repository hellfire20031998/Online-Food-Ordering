package com.hellfire.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class MessageResponse {
    private String message;

    public MessageResponse(){}
    public MessageResponse(String message) {
        this.message = message;
    }


}
