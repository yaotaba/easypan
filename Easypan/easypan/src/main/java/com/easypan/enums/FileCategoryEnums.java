package com.easypan.enums;

public enum FileCategoryEnums {
    VIDEO(1, "video", "视频"),
    MUSIC(2, "music", "音频"),
    IMAGE(3, "image", "图片"),
    DOC(4, "doc", "文档"),
    OTHERS(5, "others", "其他"),
    ALL(0,"all","全部");


    private Integer category;
    private String code;
    private String desc;

    FileCategoryEnums(Integer category, String code, String desc) {
        this.category = category;
        this.code = code;
        this.desc = desc;
    }

    public static FileCategoryEnums getByCode(String code) {
        for (FileCategoryEnums item : FileCategoryEnums.values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public Integer getCategory() {
        return category;
    }
}
