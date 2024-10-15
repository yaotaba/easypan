package com.easypan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName file_info
 */
@TableName(value = "file_info")
@Data
public class FileInfo implements Serializable {
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
    @TableField(exist = false)//不参与普通查询的映射
    private UserInfo userInfo;
}