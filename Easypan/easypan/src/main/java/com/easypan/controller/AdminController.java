package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.enums.FileCategoryEnums;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.dto.SysSettingsDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.service.FileInfoService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("admin")
public class AdminController {
    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserInfoService userInfoService;
    @RequestMapping("getSysSettings")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result getSysSettings() {
        return Result.ok(redisComponent.getSysSettingDto());
    }
    @RequestMapping("saveSysSettings")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result saveSysSettings(@VerifyParam(required = true) String registerEmailTitle,
                                  @VerifyParam(required = true) String registerEmailContent,
                                  @VerifyParam(required = true) Integer userInitUseSpace) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingDto(sysSettingsDto);
        return Result.ok(null);
    }
    @RequestMapping("loadUserList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result loadUserList  (Integer pageNo, Integer pageSize,String nickNameFuzzy,String status) {
        Result result=userInfoService.loadUserList(pageNo,pageSize,nickNameFuzzy,status);
        return result;
    }
    @RequestMapping("updateUserStatus")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result updateUserStatus  (@VerifyParam(required = true) String userId,
                                     @VerifyParam(required = true) Integer status ){
        Result result=userInfoService.updateUserStatus(userId,status);
        return result;
    }
    @RequestMapping("updateUserSpace")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result updateUserSpace  (@VerifyParam(required = true) String userId,
                                     @VerifyParam(required = true) Integer changeSpace ){
        Result result=userInfoService.updateUserSpace(userId,changeSpace);
        return result;
    }
    @PostMapping("loadFileList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result loadFileList(FileInfoQuery fileInfoQuery) {
        Result result = fileInfoService.loadFileList(fileInfoQuery);
        return result;
    }
    @PostMapping("getFolderInfo")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result getFolderInfo(@VerifyParam(required = true) String path) {
        Result result = fileInfoService.getFolderInfo(path,null);
        return result;
    }
    @RequestMapping("getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    //void是因为文件流
    public void getFile(HttpServletResponse response, @PathVariable("fileId") String fileId,@PathVariable("userId") String userId) {
        fileInfoService.getFile(response, fileId, userId);

    }
    @GetMapping("ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response, @PathVariable("fileId") String fileId, @PathVariable("userId") String userId) {
        fileInfoService.getFile(response, fileId, userId);

    }
    @RequestMapping("createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result createDownloadUrl(@PathVariable("fileId") String fileId, @PathVariable("userId") String userId) {
        Result result = fileInfoService.createDownloadUrl(fileId, userId);
        return result;
    }
    @RequestMapping("download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request,
                         HttpServletResponse response,
                         @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
        fileInfoService.download(request, response, code);
    }
    @RequestMapping("delFile")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result delFile(HttpSession session, @VerifyParam(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String [] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFile(itemArray[0],itemArray[1],true);
        }
        return Result.ok(null);
    }


}
