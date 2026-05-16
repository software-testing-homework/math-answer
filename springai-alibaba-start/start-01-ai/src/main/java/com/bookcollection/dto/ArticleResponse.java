package com.bookcollection.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Integer contentType;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    private Integer isTop;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
