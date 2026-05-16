package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookcollection.dto.CategoryResponse;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.mapper.SysCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final SysCategoryMapper categoryMapper;

    public CategoryService(SysCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAllCategories() {
        LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCategory::getStatus, 1)
                .orderByAsc(SysCategory::getSort)
                .orderByAsc(SysCategory::getId);
        
        List<SysCategory> categories = categoryMapper.selectList(wrapper);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        SysCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return toResponse(category);
    }

    public List<CategoryResponse> getCategoriesByLevel(Integer level) {
        LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCategory::getStatus, 1)
                .eq(SysCategory::getLevel, level)
                .orderByAsc(SysCategory::getSort)
                .orderByAsc(SysCategory::getId);
        
        List<SysCategory> categories = categoryMapper.selectList(wrapper);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByParentId(Long parentId) {
        LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCategory::getStatus, 1)
                .eq(SysCategory::getParentId, parentId)
                .orderByAsc(SysCategory::getSort)
                .orderByAsc(SysCategory::getId);
        
        List<SysCategory> categories = categoryMapper.selectList(wrapper);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private CategoryResponse toResponse(SysCategory category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setCode(category.getCode());
        response.setParentId(category.getParentId());
        response.setLevel(category.getLevel());
        response.setSort(category.getSort());
        response.setDescription(category.getDescription());
        response.setIcon(category.getIcon());
        response.setStatus(category.getStatus());
        return response;
    }
}
