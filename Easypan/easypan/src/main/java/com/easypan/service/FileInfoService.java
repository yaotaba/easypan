package com.easypan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.utils.Result;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;

/**
 * @author lcn
 * @description 针对表【file_info(用户信息表)】的数据库操作Service
 * @createDate 2024-09-05 16:01:32
 */
public interface FileInfoService extends IService<FileInfo> {

    QueryWrapper<FileInfo> getQueryWrapper(FileInfoQuery query,Integer category);

    Result findListByPage(FileInfoQuery fileInfoQuery,Integer category);

    Result uploadFile(SessionWebUserDto sessionWebUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    void getImage(HttpServletResponse response, String imageFolder, String imageName);

    /**
     * 获取文件，当作文件预览(通用)
      * @param response
     * @param fileId
     * @param userId
     */
    void getFile(HttpServletResponse response, String fileId, String userId);

    Result newFoloder(String filePid, String userId, String folderName);

    /**
     * 获取目录信息
     * @param path
     * @param userId
     * @return
     */
    Result getFolderInfo(String path, String userId);

    /**
     * 文件重命名
     * @param fileId
     * @param userId
     * @param fileName
     * @return
     */
    Result rename(String fileId, String userId, String fileName);

    Result loadAllFolder(String filePid, String userId, String currentFileIds);

    Result changeFileFolder(String fileIds, String filePid, String userId);

    Result createDownloadUrl(String fileId, String userId);

    void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception;

    void removeFile2RecycleBatch(String userId, String fileIds);

    /**
     * 回收站页面
     * @param fileInfoQuery
     * @return
     */
    Result loadRecycleList(FileInfoQuery fileInfoQuery);

    /**
     * 恢复数据
     * @param userId
     * @param fileIds
     * @return
     */
    Result recoverFile(String userId, String fileIds);

    /**
     * 彻底删除
     * @param userId
     * @param fileIds
     * @param
     * @return
     */
    Result delFile(String userId, String fileIds, boolean adminOp);

    /**
     * 管理员获取所有文件信息
     * @param fileInfoQuery
     * @return
     */
    Result loadFileList(FileInfoQuery fileInfoQuery);

    /**
     * 验证根目录id
     * @param rootFilePid
     * @param userId
     * @param fileId
     */

    void checkRootFilePid(String rootFilePid, String userId, String fileId);

    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUserId);

}
