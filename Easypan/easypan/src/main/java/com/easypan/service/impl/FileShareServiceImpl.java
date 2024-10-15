package com.easypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.enums.PageSize;
import com.easypan.enums.ShareValidTypeEnums;
import com.easypan.exception.MyException;
import com.easypan.exception.ParamException;
import com.easypan.exception.ShareException;
import com.easypan.mapper.FileShareMapper;
import com.easypan.pojo.FileInfo;
import com.easypan.pojo.FileShare;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionShareDto;
import com.easypan.pojo.query.FileInfoQuery;
import com.easypan.pojo.vo.ShareVO;
import com.easypan.service.FileShareService;
import com.easypan.utils.DateUtil;
import com.easypan.utils.Result;
import com.easypan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author lcn
 * @description 针对表【file_share(分享信息)】的数据库操作Service实现
 * @createDate 2024-09-18 17:12:09
 */
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare>
        implements FileShareService {
    @Resource
    private FileShareMapper fileShareMapper;

    @Override
    public Result loadShareList(FileInfoQuery fileInfoQuery) {
        int pageSize = fileInfoQuery.getPageSize() == null ? PageSize.SIZE15.getSize() : fileInfoQuery.getPageSize();
        int pageNo = fileInfoQuery.getPageNo() == null ? 1 : fileInfoQuery.getPageNo();
        FileShare fileShare = new FileShare();
        fileShare.setUserId(fileInfoQuery.getUserId());
        IPage<FileShare> page = new Page<>(pageNo, pageSize);
        fileShareMapper.selectFileSharePage(page, fileShare);
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", page.getTotal());
        map.put("pageTotal", page.getPages());
        map.put("pageSize", page.getSize());
        map.put("pageNo", page.getCurrent());
        Map<String, Object> map1 = new HashMap();
        List<ShareVO> shareVOList = new ArrayList<>();
        for (FileShare record : page.getRecords()) {
            ShareVO shareVO = new ShareVO();
            shareVO.setShareId( record.getShareId());
            shareVO.setFileId(record.getFileId());
            shareVO.setShareTime(record.getShareTime());
            shareVO.setCode(record.getCode());
            shareVO.setValidType(record.getValidType());
            shareVO.setExpireTime( record.getExpireTime());
            shareVO.setUserId(record.getUserId());
            shareVO.setShowCount( record.getShowCount());
            shareVO.setFileName(record.getFileInfo().getFileName());
            shareVO.setFolderType(record.getFileInfo().getFolderType());
            shareVO.setFileCategory(record.getFileInfo().getFileCategory());
            shareVO.setFileType(record.getFileInfo().getFileType());
            shareVO.setFileCover(record.getFileInfo().getFileCover());
            shareVOList.add(shareVO);
        }

        map.put("list", shareVOList);
        return Result.ok(map);
    }

    /**
     * 分享
     *
     * @param share
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result shareFile(FileShare share) {
        ShareValidTypeEnums typeEnums = ShareValidTypeEnums.getByType(share.getValidType());
        if (typeEnums == null) {
            throw new ParamException();
        }
        if (ShareValidTypeEnums.FOREVER != typeEnums) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));
        }
        Date curdate = new Date();
        share.setShareTime(curdate);
        if (StringTools.isEmpty(share.getCode())) {
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        share.setShowCount(0);
        fileShareMapper.insert(share);
        return Result.ok(share);
    }

    /**
     * 取消分享（删除数据）
     *
     * @param shareIdList
     * @param userId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelShare(List<String> shareIdList, String userId) {
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileShare::getShareId, shareIdList);
        wrapper.eq(FileShare::getUserId, userId);
        int delete = fileShareMapper.delete(wrapper);
        if (delete != shareIdList.size()) {
            throw new ParamException();
        }
        return Result.ok(null);
    }

    /**
     * 校验code
     * @param shareId
     * @param code
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare share = fileShareMapper.selectById(shareId);
        if(share==null||(share.getExpireTime()!=null&&new Date().after(share.getExpireTime()))){
            throw new ShareException();
        }
        if(!share.getCode().equals(code)){
            throw new MyException("提取码错误");
        }
        //更新浏览次数
        LambdaUpdateWrapper<FileShare> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("show_count=show_count+1");
        updateWrapper.eq(FileShare::getShareId,shareId);
        fileShareMapper.update(null,updateWrapper);
        SessionShareDto sessionShareDto = new SessionShareDto();
        sessionShareDto.setShareId(shareId);
        sessionShareDto.setShareUserId(share.getUserId());
        sessionShareDto.setExpireTime(share.getExpireTime());
        sessionShareDto.setFileId(share.getFileId());
        return sessionShareDto;
    }

}




