package com.easypan.enums;

/**
 * 统一返回结果状态信息类
 */
public enum ResultCodeEnum {

    SUCCESS(200, "请求成功","success"),
    PATH_NOT_FOUND(404, "path not found","error"),
    PARAM_ERROR(600, "paramError","error"),
    PARAM_USED(601, "paramUsed","error"),
    ERROR(500, "error","error"),
   SPACE_IS_SMALLER(904, "spaceIsSmaller","error"),
    SHARE_ERROR(902, "shareError","error"),
    SHARE_CODE_ERROR(903, "shareCodeError","error"),
    LOGIN_TIMEOUT(901, "loginTimeOut","error");

    private Integer code;
    private String message;
    private String status;

    private ResultCodeEnum(Integer code, String message,String status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
