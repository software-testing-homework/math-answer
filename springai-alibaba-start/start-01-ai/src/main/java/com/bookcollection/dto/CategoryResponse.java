package com.bookcollection.dto;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String description;
    private String icon;
    private Integer status;
}
