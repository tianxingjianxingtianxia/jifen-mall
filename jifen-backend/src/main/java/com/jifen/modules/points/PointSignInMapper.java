package com.jifen.modules.points;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface PointSignInMapper extends BaseMapper<PointSignIn> {

    @Select("SELECT COUNT(*) FROM wj_sign_in WHERE user_id = #{userId} AND sign_date = #{signDate} AND is_deleted = 0")
    int countByUserIdAndDate(@Param("userId") Long userId, @Param("signDate") LocalDate signDate);
}
