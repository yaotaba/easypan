package com.easypan.pojo.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.easypan.pojo.UserInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class FileInfoPageVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileId;
    private String userId;
    private String fileMd5;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    private Date createTime;
    private Date lastUpdateTime;
    private Integer folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer status;
    private Date recoveryTime;
    private Integer delFlag;
    //多表查询
    private UserInfo userInfo;
    private String nickName;
}