package com.easypan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.pojo.EmailCode;
import com.easypan.utils.Result;

/**
 * @author lcn
 * @description 针对表【email_code(邮箱验证码)】的数据库操作Service
 * @createDate 2024-08-26 11:02:23
 */
public interface EmailCodeService extends IService<EmailCode> {

    Result sendEmailcode(String email, Integer type);

    Result checkCode(String email, String emailCode);
}
