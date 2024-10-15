package com.easypan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName user_info
 */
@TableName(value = "user_info")
@Data
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private String userId;
    private String nickName;
    private String email;
    private String password;
    private Date joinTime;
    private Date lastLoginTime;
    private Integer status;
    private Long useSpace;
    private Long totalSpace;
}