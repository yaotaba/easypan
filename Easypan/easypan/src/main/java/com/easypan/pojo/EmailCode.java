package com.easypan.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName email_code
 */
@TableName(value = "email_code")
@Data
public class EmailCode implements Serializable {
    private static final long serialVersionUID = 1L;
    private String email;
    private String code;
    private Date createTime;
    private Integer status;
}