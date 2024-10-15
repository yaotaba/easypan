package com.easypan.pojo.query;

import com.easypan.enums.FileCategoryEnums;
import lombok.Data;

@Data
public class FileInfoQuery {
    private FileCategoryEnums fileCategory;
    private String fileId;
    private String filePid;
    private String fileName;
    private String fileNameFuzzy;
    private Integer pageNo;
    private Integer pageSize;
    private String userId;
    private String orderBy;
    private Integer delFlag;
}
