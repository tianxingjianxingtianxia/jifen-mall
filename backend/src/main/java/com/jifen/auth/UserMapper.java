package com.jifen.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE wj_user SET points = points + #{points}, total_earned = total_earned + #{points} WHERE id = #{userId}")
    int addPointsAndEarned(@Param("userId") Long userId, @Param("points") Integer points);

    @Update("UPDATE wj_user SET points = points - #{points}, total_spent = total_spent + #{points} WHERE id = #{userId} AND points >= #{points}")
    int deductPoints(@Param("userId") Long userId, @Param("points") Integer points);

    @Update("UPDATE wj_user SET points = points + #{points} WHERE id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("points") Integer points);

    @Update("UPDATE wj_user SET points = GREATEST(points - #{points}, 0) WHERE id = #{userId} AND points > 0")
    int expirePoints(@Param("userId") Long userId, @Param("points") Integer points);

    @Select("SELECT * FROM wj_user WHERE openid = #{openid} LIMIT 1")
    User selectByOpenId(@Param("openid") String openid);

    @Update("UPDATE wj_user SET openid = #{openid} WHERE id = #{userId}")
    int bindOpenId(@Param("userId") Long userId, @Param("openid") String openid);
}
