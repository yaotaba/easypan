package com.easypan.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/*
@JsonIgnoreProperties(ignoreUnknown = true) 是一个用于Java类的注解，
通常在使用Jackson库进行JSON序列化和反序列化时使用。它的作用是忽略JSON字符串中类中没有定义的字段。
具体来说，当你从JSON字符串反序列化（即将JSON转换为Java对象）时，如果JSON中包含的字段在Java类中没有对应的属性，
默认情况下会抛出异常或导致错误。使用这个注解后，Jackson会忽略那些在Java类中未定义的JSON字段，从而避免异常的发生。
简单总结：这个注解的作用是忽略反序列化时在Java类中不存在的JSON字段，以避免错误。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSpaceDto implements Serializable {
    private Long useSpace;
    private Long totalSpace;

    public Long getUseSpace() {
        return useSpace;
    }

    public void setUseSpace(Long useSpace) {
        this.useSpace = useSpace;
    }

    public Long getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(Long totalSpace) {
        this.totalSpace = totalSpace;
    }
}
