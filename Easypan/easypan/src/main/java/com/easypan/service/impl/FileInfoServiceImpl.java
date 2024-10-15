package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.component.RedisComponent;
import com.easypan.config.AppConfig;
import com.easypan.enums.*;
import com.easypan.exception.MyException;
import com.easypan.exception.ParamException;
import com.easypan.exception.PathException;
import com.easypan.exception.SpaceException;
import com.easypan.mapper.FileInfoMapper;
import com.easypan.mapper.UserInfoMapper;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.DownloadFileDto;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.pojo.dto.UploadResultDto;
import com.easypan.pojo.dto.UserSpaceDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.pojo.vo.FileInfoPageVo;
import com.easypan.pojo.vo.FileInfoVO;
import com.easypan.pojo.vo.FolderVO;
import com.easypan.service.FileInfoService;
import com.easypan.utils.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lcn
 * @description 针对表【file_info(用户信息表)】的数据库操作Service实现
 * @createDate 2024-09-05 16:01:32
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo>
        implements FileInfoService {
    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);
    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private AppConfig appConfig;
    @Lazy
    @Autowired
    private FileInfoServiceImpl fileInfoServiceImpl;
    private LambdaQueryWrapper<FileInfo> wrapper;

    @Override
    public QueryWrapper<FileInfo> getQueryWrapper(FileInfoQuery query, Integer category) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        if (query == null) {
            return queryWrapper;
        }
        String filePid = query.getFilePid();
        String fileId = query.getFileId();
        if(!StringUtils.isEmpty(fileId)){
            queryWrapper.eq("file_id", fileId);
        }
        if (!StringTools.isEmpty(query.getFileNameFuzzy())) {
            String fileName = query.getFileNameFuzzy();
            queryWrapper.eq("file_name", fileName);
        }
        if (category != 0&&category!=10) {
            queryWrapper.eq("file_category", category);
        }
        if (!StringTools.isEmpty(filePid)) {
            queryWrapper.eq("file_pid", filePid);
        }
        String userId = query.getUserId();
        String orderBy = query.getOrderBy();
        Integer delFlag = query.getDelFlag();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc(orderBy);
        queryWrapper.eq("del_flag", delFlag);
        return queryWrapper;
    }


    @Override
    public Result findListByPage(FileInfoQuery fileInfoQuery, Integer category) {
        int pageSize = fileInfoQuery.getPageSize() == null ? PageSize.SIZE15.getSize() : fileInfoQuery.getPageSize();
        int pageNo = fileInfoQuery.getPageNo() == null ? 1 : fileInfoQuery.getPageNo();
        Page<FileInfo> page = this.page(new Page<>(pageNo, pageSize), getQueryWrapper(fileInfoQuery, category));
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", page.getTotal());
        map.put("pageTotal", page.getPages());
        map.put("pageSize", page.getSize());
        map.put("pageNo", page.getCurrent());
        map.put("list", CopyTools.copyList(page.getRecords(), FileInfoVO.class));
        return Result.ok(map);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result uploadFile(SessionWebUserDto sessionWebUserDto, String fileId, MultipartFile file, String fileName,
                             String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            if (StringUtils.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());
            if (chunkIndex == 0) {
                QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("file_md5", fileMd5);
                queryWrapper.eq("status", FileStatusEnums.USING.getStatus());
                Page<FileInfo> page = this.page(new Page<>(0, 1), queryWrapper);
                List<FileInfo> dbFileList = page.getRecords();
                //秒传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new SpaceException("空间不足");
                    }
                    dbFile.setFileId(fileId);
                    dbFile.setFileMd5(fileMd5);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(sessionWebUserDto.getUserId());
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    //文件重命名
                    fileName = autoRename(filePid, sessionWebUserDto.getUserId(), fileName);
                    dbFile.setFileName(fileName);
                    fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    //更新用户使用空间
                    updateUserSpace(sessionWebUserDto, dbFile.getFileSize());
                    return Result.ok(resultDto);

                }
            }
            //分片上传
            //判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(sessionWebUserDto.getUserId(), fileId);
            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new SpaceException("空间不足");
            }
            //暂存临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = sessionWebUserDto.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            //file.transferTo(File dest) 会将文件的内容传输到指定的目标文件。
            file.transferTo(newFile);
            //保存临时大小
            redisComponent.saveFileTempSize(sessionWebUserDto.getUserId(), fileId, file.getSize());
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return Result.ok(resultDto);
            }
            //最后一个分片上传完成，记录数据库，异步合并分片
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(filePid, sessionWebUserDto.getUserId(), fileName);
            //存储到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(sessionWebUserDto.getUserId());
            fileInfo.setFileName(fileName);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFilePid(filePid);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnums.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfoMapper.insert(fileInfo);
            Long totalUseSize = redisComponent.getFileTempSize(sessionWebUserDto.getUserId(), fileId);
            updateUserSpace(sessionWebUserDto, totalUseSize);//更新用户使用空间
            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            //事务提交后，调用afterCommit()方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoServiceImpl.transferFile(fileInfo.getFileId(), sessionWebUserDto);
                }
            });
            return Result.ok(resultDto);

        } catch (MyException e) {
            logger.error("文件上传失败", e);
            uploadSuccess = false;
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            uploadSuccess = false;
            throw new MyException("文件上传失败");

        } finally {
            if (!uploadSuccess && tempFileFolder != null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败", e);
                    throw new MyException("删除临时目录失败");
                }
            }
        }

    }


    /**
     * 自动更新文件名
     */
    private String autoRename(String filePid, String userId, String fileName) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(FileInfo::getFilePid, filePid);
        queryWrapper.eq(FileInfo::getUserId, userId);
        queryWrapper.eq(FileInfo::getFileName, fileName);
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        Long l = fileInfoMapper.selectCount(queryWrapper);
        if (l > 0) {
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }

    /**
     * 更新空间
     */
    private void updateUserSpace(SessionWebUserDto sessionWebUserDto, Long useSpace) {
        Integer count = userInfoMapper.updateUserSpace(sessionWebUserDto.getUserId(), useSpace, null);
        if (count == 0) {//空间不足
            throw new SpaceException("空间不足");
        }
        //总空间足够，更新使用空间
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + useSpace);
        redisComponent.saveUserSpaceUse(sessionWebUserDto.getUserId(), spaceDto);
    }

    /**
     * 解码异步调用
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void transferFile(String fileId, SessionWebUserDto sessionWebUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnums = null;
        FileInfo fileInfo = fileInfoMapper.selectBYFileIdAndUserId(fileId, sessionWebUserDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            //临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = sessionWebUserDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);

            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实文件
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            //视频文件切割
            fileTypeEnums = fileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnums) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnums) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }

            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件fileId{},userId{}", fileId, sessionWebUserDto.getUserId(), e);
            transferSuccess = false;
        } finally {
            FileInfo fileInfo1 = new FileInfo();
            //new File(targetFilePath).length()返回文件大小
            fileInfo1.setFileSize(new File(targetFilePath).length());
            fileInfo1.setFileCover(cover);
            fileInfo1.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FALL.getStatus());
            LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(FileInfo::getFileId, fileInfo.getFileId());
            wrapper.eq(FileInfo::getUserId, sessionWebUserDto.getUserId());
            wrapper.eq(FileInfo::getStatus, FileStatusEnums.TRANSFER.getStatus());
            fileInfoMapper.update(fileInfo1, wrapper);
        }
    }

    /**
     * 分片合并
     *
     * @param dirPath
     * @param toFilePath
     * @param fileName
     * @param delSource
     */
    private void union(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new PathException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并文件失败", e);
                    throw new MyException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new MyException("合并文件失败" + fileName);
        } finally {
            logger.info(fileList[0] + "1111111111");
            if (writeFile != null) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 视频切片
     *
     * @param fileId
     * @param videoFilePath
     */
    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -bsf:v h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, true);
        //生成索引文件
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, true);
        //删除index.tx
        new File(tsPath).delete();
    }

    /**
     * 获取缩略图
     *
     * @param response
     * @param imageFolder
     * @param imageName
     */
    @Override
    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName) || !StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)) {
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        response.setContentType("image" + imageSuffix);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    /**
     * 获取预览文件
     *
     * @param response
     * @param fileId
     * @param userId
     */
    @Override
    public void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(FileInfo::getFileId, realFileId);
            wrapper.eq(FileInfo::getUserId, userId);
            FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
            if (fileInfo == null) {
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;

        } else {
            LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(FileInfo::getFileId, fileId);
            wrapper.eq(FileInfo::getUserId, userId);
            FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
            if (fileInfo == null) {
                return;
            }
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                //读其他为文件
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
        }

        readFile(response, filePath);
    }

    /**
     * 创建目录
     *
     * @param filePid
     * @param userId
     * @param folderName
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result newFoloder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoMapper.insert(fileInfo);
        return Result.ok(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @Override
    public Result getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        List<String> fileIdList = new ArrayList<>();
        for (int i = 0; i < pathArray.length; i++) {
            fileIdList.add(pathArray[i]);
        }
        //用in先把东西放到集合在放wrapper
        if (userId != null) {
            wrapper.eq(FileInfo::getUserId, userId);
        }
        wrapper.in(FileInfo::getFileId, fileIdList);
        wrapper.eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType());
        /*StringUtils.join(pathArray, "\",\""):
        StringUtils.join 是 Apache Commons Lang 提供的实用方法，用于将数组中的所有元素连接为一个字符串。
        pathArray 是一个字符串数组，它包含多个要加入的值。
        "\",\"" 作为分隔符，表示每个元素之间用逗号和引号 "," 分隔。*/
        String orderBy = "FIELD(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        wrapper.last("ORDER BY " + orderBy);//orderby是最后的sql
        List<FileInfo> fileInfos = fileInfoMapper.selectList(wrapper);
        return Result.ok(CopyTools.copyList(fileInfos, FolderVO.class));
    }

    /**
     * 重命名
     *
     * @param fileId
     * @param userId
     * @param fileName
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rename(String fileId, String userId, String fileName) {
        LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FileInfo::getFileId, fileId);
        wrapper.eq(FileInfo::getUserId, userId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo == null) {
            throw new MyException("文件不存在");
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        //获取文件后缀，并拼接
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbFileInfo = new FileInfo();
        dbFileInfo.setFileName(fileName);
        dbFileInfo.setLastUpdateTime(curDate);
        LambdaUpdateWrapper<FileInfo> wrapper1 = new LambdaUpdateWrapper<>();
        wrapper1.eq(FileInfo::getFileId, fileId);
        wrapper1.eq(FileInfo::getUserId, userId);
        fileInfoMapper.update(dbFileInfo, wrapper1);
        //校验数据库有无问题
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getFilePid, filePid);
        queryWrapper.eq(FileInfo::getUserId, userId);
        queryWrapper.eq(FileInfo::getFileName, fileName);
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        Long l = fileInfoMapper.selectCount(queryWrapper);
        if (l > 1) {
            throw new MyException("文件名" + fileName + "已存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return Result.ok(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    /**
     * 获取所有目录
     *
     * @param filePid
     * @param userId
     * @param currentFileIds
     * @return
     */
    @Override
    public Result loadAllFolder(String filePid, String userId, String currentFileIds) {

        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFilePid, filePid);
        wrapper.eq(FileInfo::getUserId, userId);
        if (!StringTools.isEmpty(currentFileIds)) {
            String[] split = currentFileIds.split(",");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < split.length; i++) {
                list.add(split[i]);
            }
            wrapper.notIn(FileInfo::getFileId, list);
        }
        wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        wrapper.eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType());
        wrapper.orderByDesc(FileInfo::getCreateTime);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(wrapper);
        return Result.ok(CopyTools.copyList(fileInfos, FileInfoVO.class));
    }

    /**
     * 修改文件夹位置
     *
     * @param fileIds
     * @param filePid
     * @param userId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new ParamException();
        }
        if (!Constants.LENGTH_0_STR.equals(filePid)) {
            LambdaUpdateWrapper<FileInfo> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(FileInfo::getFileId, filePid);
            wrapper1.eq(FileInfo::getUserId, userId);
            FileInfo fileInfo = fileInfoMapper.selectOne(wrapper1);
            if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new ParamException();
            }
        }
        String[] fileIdArray = fileIds.split(",");
        LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FileInfo::getFilePid, filePid);
        wrapper.eq(FileInfo::getUserId, userId);
        List<FileInfo> dbfileList = fileInfoMapper.selectList(wrapper);
        /*
        dbfileList.stream()
        将 dbfileList 转换为一个流（Stream<FileInfo>），便于进行操作。
        Collectors.toMap()
        将流中的每个 FileInfo 对象收集到一个 Map 中。
        FileInfo::getFileName
        指定 FileInfo 对象的 fileName 字段作为 Map 的 key。
        Function.identity()
        该函数返回输入的对象本身（即 FileInfo 对象），用于作为 Map 的 value。
        (data1, data2) -> data2
        当存在多个具有相同 fileName 的 FileInfo 对象时，保留第二个对象 data2，舍弃第一个 data1。
         */
        Map<String, FileInfo> dbFileNameMap = dbfileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));
        //查询选中的文件
        wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(wrapper);
        //将所选文件重命名
        for (FileInfo fileInfo : fileInfos) {
            FileInfo rootFileInfo = dbFileNameMap.get(fileInfo.getFileName());
            FileInfo updateFileInfo = new FileInfo();
            LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(fileInfo.getFileName());
                updateFileInfo.setFileName(fileName);
            }
            updateFileInfo.setFilePid(filePid);
            updateWrapper.eq(FileInfo::getFileId, fileInfo.getFileId());
            updateWrapper.eq(FileInfo::getUserId, userId);
            fileInfoMapper.update(updateFileInfo, updateWrapper);
        }
        return Result.ok(null);
    }

    /**
     * 创建下载链接
     *
     * @param fileId
     * @param userId
     * @return
     */
    @Override
    public Result createDownloadUrl(String fileId, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileId, fileId);
        wrapper.eq(FileInfo::getUserId, userId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo == null) {
            throw new ParamException();
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new ParamException();
        }
        String code = StringTools.getRandomNumber(Constants.LENGTH_50);
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setFileId(fileId);
        fileDto.setFileName(fileInfo.getFileName());
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setDownloadCode(code);
        redisComponent.saveDownloadCode(code, fileDto);
        return Result.ok(code);
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    @Override
    public void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (downloadFileDto == null) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload;charset=utf-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }

    /**
     * 删除
     *
     * @param userId
     * @param fileIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        List<String> fileIdList = Arrays.asList(fileIdArray);
        LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.in(FileInfo::getFileId, fileIdList);
        wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(wrapper);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSUbFolderFileList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.USING.getFlag());
        }
        if (!delFilePidList.isEmpty()) {
            FileInfo updateFileInfo = new FileInfo();
            updateFileInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FileInfo::getUserId, userId);
            updateWrapper.in(FileInfo::getFilePid, delFilePidList);
            updateWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
            fileInfoMapper.update(updateFileInfo, updateWrapper);
        }
        //将选中的文件放入回收站
        FileInfo updateInfo = new FileInfo();
        updateInfo.setRecoveryTime(new Date());
        updateInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        LambdaUpdateWrapper<FileInfo> updateWrapper1 = new LambdaUpdateWrapper<>();
        updateWrapper1.eq(FileInfo::getUserId, userId);
        updateWrapper1.in(FileInfo::getFileId, fileIdList);
        updateWrapper1.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        fileInfoMapper.update(updateInfo, updateWrapper1);


    }

    /**
     * 回收站页面
     *
     * @param fileInfoQuery
     * @return
     */
    @Override
    public Result loadRecycleList(FileInfoQuery fileInfoQuery) {
        int pageSize = fileInfoQuery.getPageSize() == null ? PageSize.SIZE15.getSize() : fileInfoQuery.getPageSize();
        int pageNo = fileInfoQuery.getPageNo() == null ? 1 : fileInfoQuery.getPageNo();
        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        String userId = fileInfoQuery.getUserId();
        Integer delFlag = fileInfoQuery.getDelFlag();
        wrapper.eq("del_flag", delFlag);
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("recovery_time");
        Page<FileInfo> page = this.page(new Page<>(pageNo, pageSize), wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", page.getTotal());
        map.put("pageTotal", page.getPages());
        map.put("pageSize", page.getSize());
        map.put("pageNo", page.getCurrent());
        map.put("list", CopyTools.copyList(page.getRecords(), FileInfoVO.class));
        return Result.ok(map);
    }

    /**
     * 恢复回收站数据
     *
     * @param userId
     * @param fileIds
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result recoverFile(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        List<String> fileIdList = Arrays.asList(fileIdArray);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId);
        queryWrapper.in(FileInfo::getFileId, fileIdList);
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);
        List<String> delFileSubFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSUbFolderFileList(delFileSubFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //查询所有根目录文件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        wrapper.eq(FileInfo::getFilePid, Constants.LENGTH_0_STR);
        List<FileInfo> allRooFileList = fileInfoMapper.selectList(wrapper);
        Map<String, FileInfo> rootFileMap = allRooFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));
        //恢复选中目录文件下文件的状态
        if (!delFileSubFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FileInfo::getUserId, userId);
            updateWrapper.in(FileInfo::getFilePid, delFileSubFileIdList);
            updateWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.DEL.getFlag());
            fileInfoMapper.update(fileInfo, updateWrapper);
        }
        //恢复选中文件状态
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.LENGTH_0_STR);
        fileInfo.setLastUpdateTime(new Date());
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileInfo::getUserId, userId);
        updateWrapper.in(FileInfo::getFileId, fileIdList);
        updateWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        fileInfoMapper.update(fileInfo, updateWrapper);
        //将所有选文件名字重复的重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateFileInfo = new FileInfo();
                updateFileInfo.setFileName(fileName);
                LambdaUpdateWrapper<FileInfo> updateWrapper1 = new LambdaUpdateWrapper<>();
                updateWrapper1.eq(FileInfo::getFileId, item.getFileId());
                updateWrapper1.eq(FileInfo::getUserId, userId);
                fileInfoMapper.update(updateFileInfo, updateWrapper1);
            }
        }
        return Result.ok(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delFile(String userId, String fileIds, boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");
        List<String> fileIdList = Arrays.asList(fileIdArray);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId);
        queryWrapper.in(FileInfo::getFileId, fileIdList);
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选目录的子文件目录
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSUbFolderFileList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //删除子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileInfo::getUserId, userId);
            wrapper.in(FileInfo::getFilePid, delFileSubFolderFileIdList);
            if (!adminOp) {
                wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.DEL.getFlag());
            }
            fileInfoMapper.delete(wrapper);
        }
        //删除所选文件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.in(FileInfo::getFileId, fileIdList);
        if (!adminOp) {
            wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        }
        fileInfoMapper.delete(wrapper);
        //修改使用空间
        LambdaQueryWrapper<FileInfo> wrapper1 = new LambdaQueryWrapper<>();
        Long useSpace = fileInfoMapper.selectSum(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        LambdaUpdateWrapper<UserInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfo::getUserId, userId);
        userInfoMapper.update(userInfo, updateWrapper);
        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);
        return Result.ok(null);
    }

    /**
     * 管理员获取所有文件信息
     *
     * @param fileInfoQuery
     * @return
     */
    @Override
    public Result loadFileList(FileInfoQuery fileInfoQuery) {
        int pageSize = fileInfoQuery.getPageSize() == null ? PageSize.SIZE15.getSize() : fileInfoQuery.getPageSize();
        int pageNo = fileInfoQuery.getPageNo() == null ? 1 : fileInfoQuery.getPageNo();
        IPage<FileInfo> page = new Page<>(pageNo, pageSize);
        fileInfoMapper.selectAdminFilePage(page, fileInfoQuery);
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", page.getTotal());
        map.put("pageTotal", page.getPages());
        map.put("pageSize", page.getSize());
        map.put("pageNo", page.getCurrent());
        List<Object> list = new ArrayList<>();
        for (FileInfo record : page.getRecords()) {
            FileInfoPageVo copy = CopyTools.copy(record, FileInfoPageVo.class);
            copy.setNickName(copy.getUserInfo().getNickName());
            copy.setUserInfo(null);
            list.add(copy);
        }

        map.put("list", list);
        return Result.ok(map);
    }
    /**
     * 保存到网盘
     * @param shareRootFilePid
     * @param shareFileIds
     * @param myFolderId
     * @param shareUserId
     * @param currentUserId
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUserId) {
        String[] shareFileIdArray = shareFileIds.split(",");
        List<String> shareFileIdList = Arrays.asList(shareFileIdArray);
        //目标文件列表
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId,currentUserId);
        wrapper.eq(FileInfo::getFilePid,myFolderId);
        List<FileInfo> currentFileList = fileInfoMapper.selectList(wrapper);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(),(data1,data2)->data2));
        //选择的文件
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId,shareUserId);
        queryWrapper.in(FileInfo::getFileId, shareFileIdList);
        List<FileInfo> shareFileList = fileInfoMapper.selectList(queryWrapper);
        //重命名选择的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile=currentFileMap.get(item.getFileName());
            if(haveFile!=null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList,item,shareUserId,currentUserId,curDate,myFolderId);
        }
        this.saveBatch(copyFileList);
    }
    /**
     * 递归所有文件夹下的文件
     */
    private void findAllSubFile(List<FileInfo> copyFileList,FileInfo fileInfo,String sourceUserId,String currentUserId,Date curDate,String newFilePid){
        String sourceFileId=fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId=StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileInfo::getUserId,sourceUserId);
            wrapper.eq(FileInfo::getFilePid,sourceFileId);
            List<FileInfo> sourceFileList = fileInfoMapper.selectList(wrapper);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList,item,sourceUserId,currentUserId,curDate,newFileId);
            }
        }
    }

    /**
     * 验证根目录ID
     * @param rootFilePid
     * @param userId
     * @param fileId
     */
    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if(StringTools.isEmpty(fileId)){
            throw new ParamException();
        }
        if(rootFilePid.equals(fileId)){
            return;
        }
        checkFilePid(rootFilePid,fileId,userId);

    }
    /**
     * 检查pid
     */
    private void checkFilePid(String rootFilePid,String fileId, String userId) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getFileId, fileId);
        queryWrapper.eq(FileInfo::getUserId, userId);
        FileInfo fileInfo = fileInfoMapper.selectOne(queryWrapper);
        if(fileInfo == null){
            throw new ParamException();
        }
        if(Constants.LENGTH_0_STR.equals(fileInfo.getFilePid())){
            //外面已经排除了pid=0的
            throw new ParamException();
        }
        if(rootFilePid.equals(fileInfo.getFilePid())){
            return;
        }
        checkFilePid(rootFilePid,fileInfo.getFilePid(),userId);
    }
    /**
     * 读文件
     *
     * @param response
     * @param filePath
     */
    protected void readFile(HttpServletResponse response, String filePath) {
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
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
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

    /**
     * 检查文件名
     */
    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        LambdaUpdateWrapper<FileInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.eq(FileInfo::getFileName, fileName);
        wrapper.eq(FileInfo::getFilePid, filePid);
        wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag());
        wrapper.eq(FileInfo::getFolderType, folderType);
        Long l = fileInfoMapper.selectCount(wrapper);
        if (l > 0) {
            throw new MyException("此目录下已经存在同名文件，请修改名称");
        }
    }

    /**
     * 查询所有要删除的目录包括里面目录的fileId
     */
    private void findAllSUbFolderFileList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.eq(FileInfo::getFilePid, fileId);
        wrapper.eq(FileInfo::getDelFlag, delFlag);
        wrapper.eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(wrapper);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSUbFolderFileList(fileIdList, userId, fileInfo.getFileId(), delFlag);
        }

    }
}



