package com.anmory.user.mapper;

import com.anmory.user.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户映射接口
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    /**
     * 根据登录标识查询用户,根据邮箱或者用户名二选一去查询用户
     * @param key 邮箱或者用户名
     * @return
     */
    @Select("select id, username, email, password_hash as passwordHash from users where username=#{key} or email=#{key} limit 1")
    UserEntity findByLoginKey(@Param("key") String key);
}
