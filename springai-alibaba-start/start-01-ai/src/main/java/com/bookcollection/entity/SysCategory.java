package com.bookcollection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_category")
public class SysCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private Integer level;

    private Integer sort;

    private String description;

    private String icon;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
