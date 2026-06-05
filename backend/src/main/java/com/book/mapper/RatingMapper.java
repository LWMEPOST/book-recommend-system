package com.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.book.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT user_id, book_id, score FROM rating")
    List<Map<String, Object>> selectAllRatings();
}
