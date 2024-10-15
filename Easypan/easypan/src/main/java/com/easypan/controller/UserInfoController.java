package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.config.AppConfig;
import com.easypan.enums.ResultCodeEnum;
import com.easypan.enums.VerifyRegexEnum;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.CreateImageCode;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.dto.UserSpaceDto;
import com.easypan.service.UserInfoService;
import com.easypan.utils.Result;
import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@CrossOrigin
public class UserInfoController {
    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private RedisComponent redisComponent;

    /**
     * 返回验证码图片
     *
     * @param response
     * @param session
     * @param type
     * @throws IOException
     */
    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws Exception {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @PostMapping("register")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result register(HttpSession session,
                           @VerifyParam(required = true, regx = VerifyRegexEnum.EMAIL, max = 150) String email,
                           @VerifyParam(required = true) String nickName,
                           @VerifyParam(required = true, regx = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                           @VerifyParam(required = true) String checkCode,
                           @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"验证码不正确","error");//验证码不正确
            }
            Result result = userInfoService.register(email, nickName, password, emailCode);
            return result;
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }

    @PostMapping("login")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result login(HttpSession session,
                        @VerifyParam(required = true) String email,
                        @VerifyParam(required = true) String password,
                        @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"验证码不正确","error");//验证码不正确

            }
            Result result = userInfoService.login(email, password);
            Object data = result.getData();
            SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
            if (data != null && data instanceof SessionWebUserDto) {
                sessionWebUserDto = (SessionWebUserDto) data;
                session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            }
            return result;
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }

    @PostMapping("resetPwd")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result resetPwd(HttpSession session,
                           @VerifyParam(required = true, regx = VerifyRegexEnum.EMAIL, max = 150) String email,
                           @VerifyParam(required = true, regx = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                           @VerifyParam(required = true) String checkCode,
                           @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"验证码不正确","error");//验证码不正确

            }
            Result result = userInfoService.resetPwd(email, password, emailCode);
            return result;
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }

    @GetMapping("getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getAvatar(HttpServletResponse response, @VerifyParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFAULT).exists()) {
                response.setHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_VALUE);
                response.setStatus(HttpStatus.OK.value());
                PrintWriter writer = null;
                try {
                    writer = response.getWriter();
                    writer.print("请在头像目录下放置默认头像de.jpg");
                    writer.close();
                } catch (Exception e) {
                    logger.error("输出无默认图失败", e);
                } finally {
                    writer.close();
                }
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFAULT;
        }
        response.setContentType("image/jpg");
        userInfoService.getAvatar(response, avatarPath);
    }

   /* @GetMapping("getUserInfo")
    @GlobalInterceptor
    public Result getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return Result.ok(sessionWebUserDto);
    }*/

    @PostMapping("getUseSpace")
    @GlobalInterceptor
    public Result getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());
        return Result.ok(userSpaceUse);
    }

    @RequestMapping("logout")
    public Result logout(HttpSession session) {
        session.invalidate();
        return Result.ok(null);
    }

    @PostMapping("updateUserAvatar")
    @GlobalInterceptor
    //MultipartFile 是处理上传文件的接口
    public Result updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFolder.exists()) {
            //创建指定路径的目录，包括所有不存在的父目录。如果路径中的某些父目录还不存在，mkdirs() 会一并创建这些目录。
            //如果你只需要创建单层目录，可以使用 mkdir() 方法，但它不会创建父目录。
            targetFolder.mkdirs();
        }
        File targetFile = new File(targetFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            /*
            transferTo(File dest) 方法是 MultipartFile 接口中的一个方法，用于将上传的文件内容直接转移到指定的 File 对象表示的文件中。
            当调用 avatar.transferTo(targetFile) 时，上传的文件 avatar 会被写入到 targetFile 指定的文件路径中。
            transferTo 方法会覆盖目标文件的内容，如果目标文件已经存在，之前的内容会被替换。
            在使用这个方法时，确保目标路径的目录已经存在，否则会抛出异常。
             */
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }
        return Result.ok(null);
    }

    @PostMapping("updatePassword")
    @GlobalInterceptor(checkParams = true)
    public Result updatePassword(HttpSession session,
                                 @VerifyParam(required = true, regx = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfo.setUserId(webUserDto.getUserId());
        Result result = userInfoService.updatePassword(userInfo);

        return result;
    }

}
