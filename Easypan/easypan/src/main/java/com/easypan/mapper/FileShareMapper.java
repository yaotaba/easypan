package com.easypan.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.easypan.pojo.FileShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author lcn
* @description 针对表【file_share(分享信息)】的数据库操作Mapper
* @createDate 2024-09-18 17:12:09
* @Entity com.easypan.pojo.FileShare
*/
public interface FileShareMapper extends BaseMapper<FileShare> {
    IPage<FileShare> selectFileSharePage(IPage<FileShare> page, @Param("fileShare") FileShare fileShare);

}




