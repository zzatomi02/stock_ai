package com.noono0.stock.auth.mapper;

import com.noono0.stock.auth.domain.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {

    AppUser findByUsername(@Param("username") String username);

    AppUser findById(@Param("id") long id);

    AppUser findByEmail(@Param("email") String email);

    int insert(AppUser user);

    int updatePasswordHash(@Param("id") long id, @Param("passwordHash") String passwordHash);
}
