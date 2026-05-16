package com.bookcollection.dto;

import lombok.Data;

@Data
public class CommentCreateRequest {
    private Long articleId;
    private String content;
}
