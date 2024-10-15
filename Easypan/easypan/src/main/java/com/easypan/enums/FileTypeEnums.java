package com.easypan.enums;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.IOException;

public enum FileTypeEnums {
    //1:视频2:音频3:图片4:pdf5:doc6:excel7:txt8:code9:zip10:其他'
    VIDEO(FileCategoryEnums.VIDEO,1,new String[]{".mp4",".aiv",".rmvb",".mvk",".mov"},"视频"),
    MUSIC(FileCategoryEnums.MUSIC,2,new String[]{".mp3",".wav",".wma",".mp2",".flac",".midi",".ra",".ape",".acc",".cda"},"音频"),
    IMAGE(FileCategoryEnums.IMAGE,3,new String[]{".jpeg",".jpg",".png",".gif",".bmp",".dds",".psd",".pdt",".webp",".xmp",".svg",".tiff"},"图片"),
    PDF(FileCategoryEnums.DOC,4,new String[]{".pdf"},"pdf"),
    WORD(FileCategoryEnums.DOC,5,new String[]{".docx"},"word"),
    EXCEL(FileCategoryEnums.DOC,6,new String[]{".xlsx",".xls"},"excel"),
    TXT(FileCategoryEnums.DOC,7,new String[]{".txt"},"txt"),
    PROGRAME(FileCategoryEnums.OTHERS,8,new String[]{".h",".c",".hpp",".hxx",".cpp",".cc",".c++",".cxx",".m",".o",".s",".dll",".cs",
            ".java",".class",".js",".ts",".css",".scss",".vue",".jsx",".sql",".md",".json",".html",".xml"},"CODE"),
    ZIP(FileCategoryEnums.OTHERS,9,new String[]{".rar",".zip",".7z",".cab",".ajr",".lzh",".tar",".gz",".ace",".uue",".bz",".jar",".iso",
        ".mpq"},"压缩包"),
    OTHERS(FileCategoryEnums.OTHERS,10,new String[]{},"其他")
    ;


    private FileCategoryEnums category;
    private Integer type;
    private String[] suffixs;
    private String desc;
    FileTypeEnums(FileCategoryEnums category, Integer type, String[] suffixs, String desc) {
        this.category = category;
        this.type = type;
        this.suffixs = suffixs;
        this.desc = desc;
    }
    public static FileTypeEnums getFileTypeBySuffix(String suffix) {
        for (FileTypeEnums fileTypeEnums : FileTypeEnums.values()) {
            if(ArrayUtils.contains(fileTypeEnums.getSuffixs(), suffix)) {
                return fileTypeEnums;
            }
        }
        return FileTypeEnums.OTHERS;
    }
    public static FileTypeEnums getByType(Integer type) {
        for (FileTypeEnums fileTypeEnums : FileTypeEnums.values()) {
            if(fileTypeEnums.getType().equals(type)) {
                return fileTypeEnums;
            }
        }
        return null;
    }
    public FileCategoryEnums getCategory() {
        return category;
    }

    public Integer getType() {
        return type;
    }

    public String[] getSuffixs() {
        return suffixs;
    }

    public String getDesc() {
        return desc;
    }

}
