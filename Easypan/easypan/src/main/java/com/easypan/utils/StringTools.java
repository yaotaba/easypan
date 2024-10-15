package com.easypan.utils;

import com.easypan.pojo.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {//获取随机验证码

    //返回随机数
    public static String getRandomNumber(int length) {
        String random = RandomStringUtils.random(length, false, true);
        return random;
    }
    //返回随机字符
    public static String getRandomString(int length) {
        String random = RandomStringUtils.random(length, true, true);//第一个true使返回字符，第二个返回数字
        return random;
    }

    /**
     * 如果str为空，0，null等返回true
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {//字符串去除前后空白后与“”比较
            return true;
        }
        return false;

    }

    //md5加密
    public static String encodeByMd5(String password) {
        return isEmpty(password) ? null : DigestUtils.md5Hex(password);
    }

    //判断路径是否正确
    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }

    //拿到文件名无后缀
    public static String getFileNameNoSuffix(String fileName) {
        /*
        lastIndexOf(".")：lastIndexOf(String str) 是 String 类中的方法，
        它用于查找指定字符或字符串在字符串中最后一次出现的索引位置。在这里，"." 表示要查找的字符是“点号”。
        返回值：
        如果找到了“.”字符，lastIndexOf 会返回它在 fileName 字符串中最后一次出现的位置（从 0 开始计数）。
        如果没有找到，返回值将是 -1。
         */
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        /*
        fileName：这是一个 String 对象，表示文件名。例如，"document.txt"。
        substring(0, index)：substring(int beginIndex, int endIndex) 是 String 类中的一个方法，
        它返回一个新的字符串，该字符串是从原字符串的 beginIndex 开始，
        到 endIndex 之前的所有字符（endIndex 处的字符不包含在结果中）
         */
        fileName = fileName.substring(0, index);
        return fileName;
    }

    //拿到文件后缀
    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        //substring(index)：substring(int beginIndex) 是 String 类中的一个方法，
        // 它返回从 beginIndex 开始的字符串，直到字符串的结尾。
        String suffix = fileName.substring(index);
        return suffix;
    }
    //生成随机文件名
    public static String rename(String fileName) {
        String newFileName = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return newFileName+"_"+getRandomString(Constants.LENGTH_5)+suffix;
    }
}
