package com.easypan.service;

import com.easypan.pojo.FileShare;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.pojo.dto.SessionShareDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.utils.Result;

import java.util.List;

/**
* @author lcn
* @description 针对表【file_share(分享信息)】的数据库操作Service
* @createDate 2024-09-18 17:12:09
*/
public interface FileShareService extends IService<FileShare> {
    /**
     * 分享页面
     * @param fileInfoQuery
     * @return
     */
    Result loadShareList(FileInfoQuery fileInfoQuery);

    Result shareFile(FileShare fileShare);

    Result cancelShare(List<String> shareIdList, String userId);

    /**
     * 检查code
     * @param shareId
     * @param code
     * @return
     */
    SessionShareDto checkShareCode(String shareId, String code);

}
