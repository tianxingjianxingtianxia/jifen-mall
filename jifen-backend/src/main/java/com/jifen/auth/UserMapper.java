package com.jifen.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE wj_user SET points = points + #{points}, total_earned = total_earned + #{points} WHERE id = #{userId}")
    int addPointsAndEarned(@Param("userId") Long userId, @Param("points") Integer points);
}
