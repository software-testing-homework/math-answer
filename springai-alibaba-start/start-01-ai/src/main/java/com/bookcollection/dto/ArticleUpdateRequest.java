package com.bookcollection.dto;

import lombok.Data;

@Data
public class ArticleUpdateRequest {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Integer contentType;
    private String coverImage;
    private Long categoryId;
    private String tags;
}
