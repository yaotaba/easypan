package com.easypan.pojo.constants;

public class Constants {//常量
    public static final String CHECK_CODE_KEY = "check_code_key";
    public static final String SESSION_KEY = "session_key";
    public static final String SESSION_SHARE_KEY = "session_share_key_";
    public static final String CHECK_CODE_KEY_EMAIL = "check_code_key_email";
    public static final Integer LENGTH_0 = 0;
    public static final String LENGTH_0_STR = "0";
    public static final Integer LENGTH_5 = 5;
    public static final Integer LENGTH_15 = 15;
    public static final Integer LENGTH_20 = 20;
    public static final Integer LENGTH_150 = 150;
    public static final Integer LENGTH_50 = 50;
    public static final Integer LENGTH_10 = 10;
    public static final Integer LENGTH_1 = 1;
    public static final Long MB = 1024 * 1024L;
    //Redis相关
    public static final String REDIS_KEY_SYS_SETTING = "easypan:syssetting:";
    public static final String REDIS_KEY_USER_SPACE_USE = "easypan:user:spaceuse:";
    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "easypan:user:file:temp:";
    public static final String REDIS_KEY_DOWNLOAD="easypan:download:";
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;
    public static final Integer REDIS_KEY_EXPIRES_TWO_HOUR = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 2;
    public static final Integer REDIS_KEY_EXPIRES_FIVE_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 5;
    //头像文件相关
    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_TEMP = "/temp/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";
    public static final String AVATAR_SUFFIX = ".jpg";
    public static final String IMAGE_PNG_SUFFIX = ".png";
    public static final String AVATAR_DEFAULT = "de.jpg";
    public static final String TS_NAME="index.ts";
    public static final String M3U8_NAME="index.m3u8";

    //response相关
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";


}
