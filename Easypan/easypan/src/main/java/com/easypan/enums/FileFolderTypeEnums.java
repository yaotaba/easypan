package com.easypan.enums;

public enum FileFolderTypeEnums {
    FILE(0,"文件"),
    FOLDER(1,"目录")
    ;

    private Integer type;
    private String desc;

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    FileFolderTypeEnums(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
