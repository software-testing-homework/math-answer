package com.bookcollection.dto;

import lombok.Data;

@Data
public class ConversationListResponse {
    private Long id;
    private String title;
    private String description;
    private Integer messageCount;
    private String lastMessage;
    private Integer isStar;
}
