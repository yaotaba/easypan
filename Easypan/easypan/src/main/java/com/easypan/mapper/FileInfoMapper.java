package com.easypan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.query.FileInfoQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lcn
 * @description 针对表【file_info(用户信息表)】的数据库操作Mapper
 * @createDate 2024-09-05 16:01:32
 * @Entity com.easypan.pojo.FileInfo
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {
    Long selectSum(@Param("userId") String userId);

    FileInfo selectBYFileIdAndUserId(@Param("fileId") String fileId, @Param("userId")String userId);
    IPage<FileInfo> selectAdminFilePage(IPage<FileInfo> page, @Param("query")FileInfoQuery query);
}




