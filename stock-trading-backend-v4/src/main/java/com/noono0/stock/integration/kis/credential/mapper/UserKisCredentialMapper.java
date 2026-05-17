package com.noono0.stock.integration.kis.credential.mapper;

import com.noono0.stock.integration.kis.credential.domain.UserKisCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserKisCredentialMapper {

    UserKisCredential findByUserIdAndMode(@Param("userId") String userId, @Param("mode") String mode);

    int insert(UserKisCredential row);

    int updateById(UserKisCredential row);

    int deleteByUserIdAndMode(@Param("userId") String userId, @Param("mode") String mode);
}
