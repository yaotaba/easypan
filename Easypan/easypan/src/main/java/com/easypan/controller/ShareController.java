package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.pojo.FileShare;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.service.FileShareService;
import com.easypan.utils.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/share")
public class ShareController {
    @Resource
    private FileShareService fileShareService;
    @RequestMapping("loadShareList")
    @GlobalInterceptor
    public Result loadShareList(HttpSession session, Integer pageNo, Integer pageSize) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoQuery.setPageSize(pageSize);
        fileInfoQuery.setPageNo(pageNo);
        fileInfoQuery.setUserId(sessionWebUserDto.getUserId());
        Result result = fileShareService.loadShareList(fileInfoQuery);
        return result;
    }
    @RequestMapping("shareFile")
    @GlobalInterceptor(checkParams = true)
    public Result shareFile(HttpSession session,
                            @VerifyParam(required = true) String fileId,
                            @VerifyParam(required = true) Integer validType,
                            String code) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileShare fileShare = new FileShare();
        fileShare.setValidType(validType);
        fileShare.setCode(code);
        fileShare.setFileId(fileId);
        fileShare.setUserId(sessionWebUserDto.getUserId());
        Result result = fileShareService.shareFile(fileShare);
        return result;
    }
    @RequestMapping("cancelShare")
    @GlobalInterceptor(checkParams = true)
    public Result cancelShare(HttpSession session,
                            @VerifyParam(required = true) String shareIds) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
       String[] shareIdArray = shareIds.split(",");
       List<String> shareIdList = Arrays.asList(shareIdArray);
        Result result = fileShareService.cancelShare(shareIdList,sessionWebUserDto.getUserId());
        return result;
    }


}
