package com.easypan.annotation;

import com.easypan.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface VerifyParam {
    int min() default -1;//最小值

    int max() default -1;//最大值

    boolean required() default false;//是否必须传递

    VerifyRegexEnum regx() default VerifyRegexEnum.No;//是否校验正则

}
