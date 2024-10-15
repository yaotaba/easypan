package com.easypan.utils;

import com.easypan.enums.VerifyRegexEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//校验正则
public class VerifyUtils {
    public static boolean verify(String regex, String value) {
        if (StringTools.isEmpty(value)) {
            return false;
        }
        //Pattern 是 Java 的一个类，用于编译正则表达式,编译成一个 Pattern 对象
        Pattern pattern = Pattern.compile(regex);
        //Matcher 是 Java 的一个类，用于对输入的字符串 value 执行匹配操作。pattern.matcher(value) 方法-
        // 创建一个 Matcher 对象，该对象用于在 value 字符串中查找与 Pattern 对象中的模式匹配的部分。
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    public static boolean verify(VerifyRegexEnum regex, String value) {
        return verify(regex.getRegex(), value);
    }
}
