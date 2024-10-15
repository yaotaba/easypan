package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.enums.FileCategoryEnums;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.service.FileInfoService;
import com.easypan.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@CrossOrigin
@RequestMapping("/recycle")
public class RecycleController {
    @Autowired
    private FileInfoService fileInfoService;

    @RequestMapping("loadRecycleList")
    @GlobalInterceptor
    public Result loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoQuery.setPageSize(pageSize);
        fileInfoQuery.setPageNo(pageNo);
       fileInfoQuery.setUserId(sessionWebUserDto.getUserId());
       fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        Result result = fileInfoService.loadRecycleList(fileInfoQuery);
        return result;
    }
    @RequestMapping("recoverFile")
    @GlobalInterceptor(checkParams = true)
    public Result recoverFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.recoverFile(sessionWebUserDto.getUserId(),fileIds);
        return result;
    }
    @RequestMapping("delFile")
    @GlobalInterceptor(checkParams = true)
    public Result delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.delFile(sessionWebUserDto.getUserId(),fileIds,false);
        return result;
    }

}
