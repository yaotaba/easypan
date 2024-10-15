package com.easypan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easypan.pojo.EmailCode;
import org.apache.ibatis.annotations.Param;

/**
 * @author lcn
 * @description 针对表【email_code(邮箱验证码)】的数据库操作Mapper
 * @createDate 2024-08-26 11:02:23
 * @Entity com.easypan.pojo.EmailCode
 */
public interface EmailCodeMapper extends BaseMapper<EmailCode> {

    EmailCode selectByEmailAndCode(@Param("email") String email, @Param("code") String code);
}




