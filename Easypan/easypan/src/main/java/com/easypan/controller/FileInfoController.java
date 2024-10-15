package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.config.AppConfig;
import com.easypan.enums.FileCategoryEnums;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.service.FileInfoService;
import com.easypan.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@CrossOrigin
@RequestMapping("file")
public class FileInfoController {
    @Autowired
    private FileInfoService fileInfoService;
    @Autowired
    private AppConfig appConfig;

    /**
     * 分页查询，文件列表
     *
     * @param session
     * @param fileInfoQuery
     * @param category
     * @return
     */
    @PostMapping("loadDataList")
    @GlobalInterceptor
    public Result loadDataList(HttpSession session, FileInfoQuery fileInfoQuery, String category) {
        /*Enumeration e=request.getParameterNames();//获取所有参数名
        while(e.hasMoreElements()){//通过Enumeration类中的hasMoreElements()判断是否还有参数名
            String parameterName=(String)e.nextElement(); //获取当前参数名
            //再通过request.getParameter("")的方法来获取对应参数名的值
            System.out.println(parameterName+": "+request.getParameter(parameterName));
        }

        System.out.println("-----"+request+"----");*/
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        Integer category1 = 0;
        if (categoryEnum != null) {
            fileInfoQuery.setFileCategory(categoryEnum);
            category1 = categoryEnum.getCategory();
        }
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoQuery.setUserId(sessionWebUserDto.getUserId());
        fileInfoQuery.setOrderBy("last_update_time");
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Result result = fileInfoService.findListByPage(fileInfoQuery, category1);
        return result;
    }

    /**
     * 上传文件
     *
     * @param session
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    @PostMapping("uploadFile")
    @GlobalInterceptor(checkParams = true)
    public Result uploadFile(HttpSession session,
                             String fileId,
                             MultipartFile file,
                             @VerifyParam(required = true) String fileName,
                             @VerifyParam(required = true) String filePid,
                             @VerifyParam(required = true) String fileMd5,
                             @VerifyParam(required = true) Integer chunkIndex,
                             @VerifyParam(required = true) Integer chunks
    ) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        Result result = fileInfoService.uploadFile(sessionWebUserDto, fileId, file, fileName,
                filePid, fileMd5, chunkIndex, chunks);
        return result;
    }

    /**
     * 获取缩略图
     *
     * @param response
     * @param imageFolder
     * @param imageName
     */
    @GetMapping("getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        fileInfoService.getImage(response, imageFolder, imageName);

    }

    /**
     * 视频预览
     *
     * @param response
     * @param fileId
     * @param session
     */
    @GetMapping("ts/getVideoInfo/{fileId}")
    @GlobalInterceptor
    public void getVideoInfo(HttpServletResponse response, @PathVariable("fileId") String fileId, HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.getFile(response, fileId, sessionWebUserDto.getUserId());

    }

    /**
     * 获取除视频外的文件预览
     *
     * @param response
     * @param fileId
     * @param session
     */
    @RequestMapping("getFile/{fileId}")
    @GlobalInterceptor
    //void是因为文件流
    public void getFile(HttpServletResponse response, @PathVariable("fileId") String fileId, HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.getFile(response, fileId, sessionWebUserDto.getUserId());

    }

    /**
     * 创建目录
     *
     * @param session
     * @param filePid
     * @param fileName
     * @return
     */
    @RequestMapping("newFoloder")
    @GlobalInterceptor(checkParams = true)
    //目录本质上也是文件
    public Result newFoloder(HttpSession session,
                             @VerifyParam(required = true) String filePid,
                             @VerifyParam(required = true) String fileName) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.newFoloder(filePid, sessionWebUserDto.getUserId(), fileName);
        return result;

    }

    /**
     * 获取当前文件目录
     */
    @RequestMapping("getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    //目录本质上也是文件
    public Result getFolderInfo(HttpSession session,
                                @VerifyParam(required = true) String path) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.getFolderInfo(path, sessionWebUserDto.getUserId());
        return result;

    }

    /**
     * 文件重命名
     */
    @RequestMapping("rename")
    @GlobalInterceptor(checkParams = true)
    //目录本质上也是文件
    public Result rename(HttpSession session,
                         @VerifyParam(required = true) String fileId,
                         @VerifyParam(required = true) String fileName) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.rename(fileId, sessionWebUserDto.getUserId(), fileName);
        return result;

    }

    /**
     * 获取所有目录
     */
    @RequestMapping("loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    //目录本质上也是文件
    public Result loadAllFolder(HttpSession session,
                                @VerifyParam(required = true) String filePid,
                                String currentFileIds) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.loadAllFolder(filePid, sessionWebUserDto.getUserId(), currentFileIds);
        return result;

    }

    /**
     * 移动文件
     */
    @RequestMapping("changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    //目录本质上也是文件
    public Result changeFileFolder(HttpSession session,
                                   @VerifyParam(required = true) String fileIds,
                                   @VerifyParam(required = true) String filePid) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.changeFileFolder(fileIds, filePid, sessionWebUserDto.getUserId());
        return result;

    }

    /**
     * 创建下载链接
     */
    @RequestMapping("createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public Result createDownloadUrl(HttpSession session,
                                    @VerifyParam(required = true) @PathVariable("fileId") String fileId) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        Result result = fileInfoService.createDownloadUrl(fileId, sessionWebUserDto.getUserId());
        return result;
    }

    /**
     * 下载文件
     */
    @RequestMapping("download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request,
                         HttpServletResponse response,
                         @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
        fileInfoService.download(request, response, code);
    }

    /**
     * 删除文件
     */
    @RequestMapping("delFile")
    @GlobalInterceptor(checkParams = true)
    public Result delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto sessionWebUserDto= (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.removeFile2RecycleBatch(sessionWebUserDto.getUserId(), fileIds);
        return Result.ok(null);
    }
}

