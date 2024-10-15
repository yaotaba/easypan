package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.component.RedisComponent;
import com.easypan.config.AppConfig;
import com.easypan.enums.ResultCodeEnum;
import com.easypan.exception.MyException;
import com.easypan.exception.ParamException;
import com.easypan.mapper.EmailCodeMapper;
import com.easypan.mapper.UserInfoMapper;
import com.easypan.pojo.EmailCode;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SysSettingsDto;
import com.easypan.service.EmailCodeService;
import com.easypan.utils.Result;
import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * @author lcn
 * @description 针对表【email_code(邮箱验证码)】的数据库操作Service实现
 * @createDate 2024-08-26 11:02:23
 */
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode>
        implements EmailCodeService {
    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);//日志
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private EmailCodeMapper emailCodeMapper;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private RedisComponent redisComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    //必须要抛出异常
    public Result sendEmailcode(String email, Integer type) {
        if (type == Constants.LENGTH_0) {
            //注册先判断有没有被占用
            LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper();
            wrapper.eq(UserInfo::getEmail, email);
            long l = userInfoMapper.selectCount(wrapper);
            if (l != Constants.LENGTH_0) {
                return Result.build(null, ResultCodeEnum.PARAM_USED.getCode(),"邮箱已存在","error");
            }
        }
        String code = StringTools.getRandomNumber(Constants.LENGTH_5);
        //发送验证码
        try {
            sendEmailCode(email, code);
        } catch (Exception e) {
            logger.error("发送失败");
            throw new MyException("邮件发送失败");
        }
        //每次插入前先根据邮箱把所有验证码状态置为已使用
        EmailCode emailCode = new EmailCode();
        disableStatus(emailCode, email);
        //插入邮箱验证码信息
        emailCode.setCode(code);
        emailCode.setStatus(Constants.LENGTH_0);//未使用
        emailCode.setCreateTime(new Date());
        emailCodeMapper.insert(emailCode);
        return Result.ok(null);
    }

    /**
     * 根据邮箱把所有验证码状态置为已使用
     *
     * @param emailCode
     * @param email
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableStatus(EmailCode emailCode, String email) {
        emailCode.setEmail(email);
        emailCode.setStatus(Constants.LENGTH_1);
        LambdaQueryWrapper<EmailCode> wrapper1 = new LambdaQueryWrapper();
        wrapper1.eq(EmailCode::getEmail, email);
        wrapper1.eq(EmailCode::getStatus, Constants.LENGTH_0);
        emailCodeMapper.update(emailCode, wrapper1);
    }

    /**
     * 发送邮件
     *
     * @param toEmail 目标邮箱
     * @param code    验证码
     */
    private void sendEmailCode(String toEmail, String code) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(appConfig.getSendUsername());
        helper.setTo(toEmail);
        //获取邮件内容
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingDto();
        helper.setSubject(sysSettingsDto.getRegisterEmailTitle());//标题
        helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(), code));//内容String.format()
        helper.setSentDate(new Date());
        javaMailSender.send(mimeMessage);

    }

    /**
     * 检查邮箱验证码
     *
     * @param email
     * @param Code
     */
    @Override
    public Result checkCode(String email, String Code) {
        EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email, Code);
        if (emailCode == null) {
            throw new ParamException("邮箱验证码不正确");
        }
        if (emailCode.getStatus() == 1 || System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.LENGTH_15 * 1000 * 60) {
            throw new ParamException("邮箱验证码不正确");
        }
        disableStatus(emailCode, email);
        return Result.ok(null);
    }

}





