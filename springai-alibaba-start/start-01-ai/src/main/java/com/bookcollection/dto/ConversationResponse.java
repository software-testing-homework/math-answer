package com.bookcollection.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime lastMessageTime;
    private Integer isStar;
    private List<MessageResponse> messages;
}

