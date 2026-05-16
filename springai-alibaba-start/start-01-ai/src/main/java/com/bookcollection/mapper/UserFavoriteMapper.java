package com.bookcollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookcollection.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    @Select("SELECT * FROM user_favorite WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    UserFavorite findByUserAndTarget(@Param("userId") Long userId, @Param("targetType") Integer targetType, @Param("targetId") Long targetId);

    @Select("SELECT target_id FROM user_favorite WHERE user_id = #{userId} AND target_type = 3 ORDER BY create_time DESC LIMIT #{limit}")
    List<Long> findFavoriteArticleIds(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM user_favorite WHERE user_id = #{userId} AND target_type = 3")
    Integer countFavoriteArticles(@Param("userId") Long userId);
}
