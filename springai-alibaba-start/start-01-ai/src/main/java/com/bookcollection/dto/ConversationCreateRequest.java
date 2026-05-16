package com.bookcollection.dto;

import lombok.Data;

@Data
public class ConversationCreateRequest {
    private String title;
    private String firstMessage;
}
