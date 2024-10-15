package com.easypan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.enums.FileCategoryEnums;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.enums.FileStatusEnums;
import com.easypan.exception.MyException;
import com.easypan.exception.ShareCodeException;
import com.easypan.exception.ShareException;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.FileShare;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionShareDto;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.pojo.vo.ShareInfoVO;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.Result;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("showShare")
public class WebShareController {
    @Resource
    private FileShareService fileShareService;
    @Resource
    private UserInfoService userInfoService;
    @Resource
    private FileInfoService fileInfoService;
    @RequestMapping("getShareLoginInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionShareDto sessionShareDto=(SessionShareDto)session.getAttribute(Constants.SESSION_SHARE_KEY+shareId);
        if(sessionShareDto==null){
            return Result.ok(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        SessionWebUserDto sessionWebUserDto=(SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if(sessionWebUserDto!=null&&sessionWebUserDto.getUserId().equals(sessionShareDto.getShareUserId())){
            shareInfoVO.setCurrentUser(true);
        }else {
            shareInfoVO.setCurrentUser(false);
        }
        return Result.ok(shareInfoVO);
    }
    @RequestMapping("getShareInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result getShareInfo( @VerifyParam(required = true) String shareId) {
        ShareInfoVO result =getShareInfoCommon(shareId);
        return Result.ok(result);
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare share = fileShareService.getById(shareId);
        if(share==null||(share.getExpireTime()!=null&&new Date().after(share.getExpireTime()))){
            throw new ShareException();
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, share.getUserId());
        wrapper.eq(FileInfo::getFileId, share.getFileId());
        FileInfo fileInfo = fileInfoService.getOne(wrapper);
        if(fileInfo==null||!FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())){
            throw new ShareException();
        }
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getById(share.getUserId());
        shareInfoVO.setAvatar(null);
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验code，并且获取SessionShareDto
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("checkShareCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result checkShareCode(HttpSession session, @VerifyParam(required = true) String shareId,
                                                      @VerifyParam(required = true) String code ) {
        SessionShareDto sessionShareDto=fileShareService.checkShareCode(shareId,code);
        session.setAttribute(Constants.SESSION_SHARE_KEY+shareId,sessionShareDto);
        return Result.ok(null);
    }

    /**
     * 加载分享文件
     * @param session
     * @param shareId
     * @param filePid
     * @return
     */
    @PostMapping("loadFileList")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result loadFileList(HttpSession session, @VerifyParam(required = true) String shareId,String filePid) {
        SessionShareDto sessionShareDto=checkShare(session,shareId);
        FileInfoQuery fileInfoQuery=new FileInfoQuery();
        if(!StringTools.isEmpty(filePid)&&!Constants.LENGTH_0_STR.equals(filePid)){
            fileInfoService.checkRootFilePid(sessionShareDto.getFileId(),sessionShareDto.getShareUserId(),filePid);
            fileInfoQuery.setFilePid(filePid);
        }else {
            fileInfoQuery.setFileId(sessionShareDto.getFileId());
        }
        fileInfoQuery.setUserId(sessionShareDto.getShareUserId());
        fileInfoQuery.setOrderBy("last_update_time");
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer category=10;//此时为category=null
        Result result = fileInfoService.findListByPage(fileInfoQuery, category);
        return result;
    }

    /**
     * 校验分享链接
     * @param session
     * @param shareId
     * @return
     */
    private SessionShareDto checkShare(HttpSession session,String shareId)
    {
        SessionShareDto sessionShareDto=(SessionShareDto)session.getAttribute(Constants.SESSION_SHARE_KEY+shareId);
        if(sessionShareDto==null){
            throw new ShareCodeException();
        }
        if(sessionShareDto.getExpireTime()!=null&&new Date().after(sessionShareDto.getExpireTime())){
            throw new ShareException();
        }
        return sessionShareDto;
    }
    /**
     * 获取当前文件目录
     */
    @RequestMapping("getFolderInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    //目录本质上也是文件
    public Result getFolderInfo(HttpSession session,
                                @VerifyParam(required = true) String shareId,
                                @VerifyParam(required = true) String path) {
      SessionShareDto sessionShareDto=checkShare(session,shareId);
        Result result = fileInfoService.getFolderInfo(path, sessionShareDto.getShareUserId());
        return result;
    }
    @RequestMapping("getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    //void是因为文件流
    public void getFile(HttpServletResponse response,
                        HttpSession session,
                        @PathVariable("fileId") String fileId,
                        @PathVariable("shareId") String shareId) {
        SessionShareDto sessionShareDto=checkShare(session,shareId);
        fileInfoService.getFile(response, fileId, sessionShareDto.getShareUserId());

    }
    @GetMapping("ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void getVideoInfo(HttpServletResponse response,
                             HttpSession session,
                             @PathVariable("fileId") String fileId,
                             @PathVariable("shareId") String shareId) {
        SessionShareDto sessionShareDto=checkShare(session,shareId);
        fileInfoService.getFile(response, fileId, sessionShareDto.getShareUserId());

    }
    @RequestMapping("createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result createDownloadUrl(@PathVariable("fileId") String fileId,
                                    @PathVariable("shareId") String shareId,
                                    HttpSession session) {
        SessionShareDto sessionShareDto=checkShare(session,shareId);
        Result result = fileInfoService.createDownloadUrl(fileId, sessionShareDto.getShareUserId());
        return result;
    }
    @RequestMapping("download/{code}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void download(HttpServletRequest request,
                         HttpServletResponse response,
                         @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
        fileInfoService.download(request, response, code);
    }

    /**
     * 保存到我的网盘
     *
     */
    @RequestMapping("saveShare")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result saveShare(HttpSession session,
                         @VerifyParam(required = true)  String shareId,
                         @VerifyParam(required = true)  String shareFileIds,
                         @VerifyParam(required = true)  String myFolderId) {
        SessionShareDto sessionShareDto=checkShare(session,shareId);
        SessionWebUserDto sessionWebUserDto=(SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
        if(sessionShareDto.getShareUserId().equals(sessionWebUserDto.getUserId())){
            throw new MyException("自己分享的为文件无法保存到自己的网盘");
        }
        fileInfoService.saveShare(sessionShareDto.getFileId(),shareFileIds,myFolderId,sessionShareDto.getShareUserId(),sessionWebUserDto.getUserId());
        return Result.ok(null);
    }
}
