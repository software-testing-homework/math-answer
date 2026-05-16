package com.bookcollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookcollection.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
