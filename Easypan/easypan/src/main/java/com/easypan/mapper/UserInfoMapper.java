package com.easypan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easypan.pojo.UserInfo;
import org.apache.ibatis.annotations.Param;

/**
 * @author lcn
 * @description 针对表【user_info(用户信息表)】的数据库操作Mapper
 * @createDate 2024-08-25 15:50:35
 * @Entity com.easypan.pojo.UserInfo
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {
    Integer updateUserSpace(@Param("userId")String userId, @Param("useSpace")Long useSpace
                                    ,@Param("totalSpace")Long totalSpace);

}




