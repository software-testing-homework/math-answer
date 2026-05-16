package com.bookcollection.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private Long articleId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isLiked;
}
