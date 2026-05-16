package com.bookcollection.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId;
    private String message;
    private Integer modelType;
}
