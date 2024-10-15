package com.easypan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.pojo.UserInfo;
import com.easypan.utils.Result;

import javax.servlet.http.HttpServletResponse;

/**
 * @author lcn
 * @description 针对表【user_info(用户信息表)】的数据库操作Service
 * @createDate 2024-08-25 15:50:35
 */
public interface UserInfoService extends IService<UserInfo> {

    Result register(String email, String nickName, String password, String emailCode);

    Result login(String email, String password);

    Result resetPwd(String email, String password, String emailCode);

    void getAvatar(HttpServletResponse response, String filePath);

    Result updatePassword(UserInfo userInfod);

    Result loadUserList(Integer pageNo, Integer pageSize, String nickNameFuzzy, String status);

    Result updateUserStatus(String userId, Integer status);

    Result updateUserSpace(String userId, Integer changeSpace);
}
