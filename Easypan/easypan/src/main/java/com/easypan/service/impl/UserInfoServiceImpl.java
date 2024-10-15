package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.component.RedisComponent;
import com.easypan.config.AppConfig;
import com.easypan.enums.PageSize;
import com.easypan.enums.ResultCodeEnum;
import com.easypan.enums.UserStatusEnum;
import com.easypan.mapper.FileInfoMapper;
import com.easypan.mapper.UserInfoMapper;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.dto.UserSpaceDto;
import com.easypan.pojo.vo.FileInfoVO;
import com.easypan.pojo.vo.UserInfoVO;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.Result;
import com.easypan.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lcn
 * @description 针对表【user_info(用户信息表)】的数据库操作Service实现
 * @createDate 2024-08-25 15:50:35
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
        implements UserInfoService {
    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 注册
     *
     * @param email
     * @param nickName
     * @param password
     * @param emailCode
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(String email, String nickName, String password, String emailCode) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<UserInfo> wrapper1 = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getEmail, email);
        wrapper1.eq(UserInfo::getNickName, nickName);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        UserInfo userInfo1 = userInfoMapper.selectOne(wrapper1);
        if (userInfo != null) {
            return Result.build(null, ResultCodeEnum.PARAM_USED.getCode(),"邮箱已存在","error");
        }
        if (userInfo1 != null) {
            return Result.build(null, ResultCodeEnum.PARAM_USED.getCode(),"用户名已存在","error");
        }
        Result result = emailCodeService.checkCode(email, emailCode);
        String userId = StringTools.getRandomNumber(Constants.LENGTH_15);
        UserInfo userInfo2 = new UserInfo();
        userInfo2.setEmail(email);
        userInfo2.setNickName(nickName);
        userInfo2.setPassword(StringTools.encodeByMd5(password));
        userInfo2.setUserId(userId);
        userInfo2.setJoinTime(new Date());
        userInfo2.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo2.setUseSpace(0L);//使用空间
        userInfo2.setTotalSpace(redisComponent.getSysSettingDto().getUserInitUseSpace() * Constants.MB);
        userInfoMapper.insert(userInfo2);
        return result;

    }

    /**
     * 登录
     *
     * @param email
     * @param password
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result login(String email, String password) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        if (userInfo == null || !userInfo.getPassword().equals(password)) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"账号或密码错误","error");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"账号被禁用","error");
        }
        //修改最后登录时间
        UserInfo updateUserInfo = new UserInfo();
        updateUserInfo.setUserId(userInfo.getUserId());
        updateUserInfo.setLastLoginTime(new Date());
        userInfoMapper.updateById(updateUserInfo);
        //设置SessionWebUserDto
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(userInfo.getUserId());
        sessionWebUserDto.setNickName(userInfo.getNickName());
        //设置管理员
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionWebUserDto.setAdmin(true);
        } else {
            sessionWebUserDto.setAdmin(false);
        }
        //设置空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        //userSpaceDto.setUserSpace();//根据用户文件设置,后期补上
        Long userSpace=fileInfoMapper.selectSum(userInfo.getUserId());
        userSpaceDto.setUseSpace(userSpace);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        //保存空间在内存
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return Result.ok(sessionWebUserDto);
    }

    /**
     * 重置密码
     *
     * @param email
     * @param password
     * @param emailCode
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result resetPwd(String email, String password, String emailCode) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        if (userInfo == null) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"邮箱不存在","error");
        }
        if (userInfo.getPassword().equals(StringTools.encodeByMd5(password))) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"新密码与旧密码一致","error");
        }
        //检查邮箱验证码
        emailCodeService.checkCode(email, emailCode);
        //设置新密码
        UserInfo updateUserInfo = new UserInfo();
        updateUserInfo.setUserId(userInfo.getUserId());
        updateUserInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoMapper.updateById(updateUserInfo);
        return Result.ok(null);
    }

    /**
     * 获取头像（读流）
     *
     * @param response
     * @param filePath
     */
    @Override
    public void getAvatar(HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            in = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updatePassword(UserInfo userInfo) {
        UserInfo userInfo1 = userInfoMapper.selectById(userInfo.getUserId());
        if (userInfo1.getPassword().equals(userInfo.getPassword())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"新密码与旧密码一致","error");

        }
        userInfoMapper.updateById(userInfo);
        return Result.ok(null);
    }

    /**
     * 获取用户列表
     * @param pageNo
     * @param pageSize
     * @param nickNameFuzzy
     * @param status
     * @return
     */
    @Override
    public Result loadUserList(Integer pageNo, Integer pageSize, String nickNameFuzzy, String status) {
        if (pageSize == null) {
            pageSize = PageSize.SIZE15.getSize();
        }
        if (pageNo == null) {
            pageNo= 1;
        }
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if(status!=null){
            wrapper.eq(UserInfo::getStatus,status);
        }
        if(nickNameFuzzy!=null){
            wrapper.like(UserInfo::getNickName,nickNameFuzzy);
        }
        wrapper.orderByDesc(UserInfo::getJoinTime);
        Page page = this.page(new Page(pageNo, pageSize), wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", page.getTotal());
        map.put("pageTotal", page.getPages());
        map.put("pageSize", page.getSize());
        map.put("pageNo", page.getCurrent());
        map.put("list", CopyTools.copyList(page.getRecords(), UserInfoVO.class));
        return Result.ok(map);
    }

    /**
     * 更新用户状态
     * @param userId
     * @param status
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateUserStatus(String userId, Integer status) {
        UserInfo userInfo=new UserInfo();
        userInfo.setStatus(status);
        userInfo.setUserId(userId);
        if(UserStatusEnum.DISABLE.getStatus().equals(status)){
            //禁用后，空间和文件清空
            userInfo.setUseSpace(0L);
            LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileInfo::getUserId,userId);
            fileInfoMapper.delete(wrapper);
        }
        userInfoMapper.updateById(userInfo);
        return Result.ok(null);
    }

    /**
     * 修改用户空间
     * @param userId
     * @param changeSpace
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateUserSpace(String userId, Integer changeSpace) {
        Long space=changeSpace*Constants.MB;
        userInfoMapper.updateUserSpace(userId,null,space);
        redisComponent.resetUserSpaceUse(userId);
        return Result.ok(null);
    }

}




