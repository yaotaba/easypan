package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.enums.ResultCodeEnum;
import com.easypan.enums.VerifyRegexEnum;
import com.easypan.pojo.constants.Constants;
import com.easypan.service.EmailCodeService;
import com.easypan.utils.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@CrossOrigin
public class EmailCodeController {
    @Resource
    private EmailCodeService emailCodeService;

    @PostMapping("sendEmailCode")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result sendEmailCode(HttpSession session,
                                @VerifyParam(required = true, regx = VerifyRegexEnum.EMAIL, max = 150) String email,
                                @VerifyParam(required = true) String checkCode,
                                @VerifyParam(required = true) Integer type) {  //type用来判断注册还是找回密码
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"验证码不正确",ResultCodeEnum.PARAM_ERROR.getStatus());//验证码不正确
            }

            Result result = emailCodeService.sendEmailcode(email, type);
            return result;
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }

    }
}
