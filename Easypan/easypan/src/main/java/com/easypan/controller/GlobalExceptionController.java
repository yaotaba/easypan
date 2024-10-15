package com.easypan.controller;

import com.easypan.enums.ResultCodeEnum;
import com.easypan.exception.*;
import com.easypan.utils.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice//全局异常处理
public class GlobalExceptionController {
//    @ExceptionHandler(value = Exception.class)//该注解标记异常处理Handler,并且指定发生异常调用该方法!
//    public Result exception(Exception e) {
//        return Result.build("程序出错", ResultCodeEnum.ERROR);
//    }

//    @ExceptionHandler(value = RuntimeException.class)
//    public Result runtimeException(RuntimeException e) {
//        return Result.build("程序出错", ResultCodeEnum.ERROR);
//    }

    @ExceptionHandler(value = ParamException.class)
    public Result paramException(ParamException e) {
        return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"参数错误","error");
    }

    @ExceptionHandler(value = LoginOutException.class)
    public Result LoginOutException(LoginOutException e) {
        return Result.build(null, ResultCodeEnum.LOGIN_TIMEOUT.getCode(),"登陆超时","error");
    }

    @ExceptionHandler(value = PathException.class)
    public Result PathException(PathException e) {
        return Result.build(null, ResultCodeEnum.PATH_NOT_FOUND.getCode(),"路径错误","error");
    }
    @ExceptionHandler(value = SpaceException.class)
    public Result SpaceException(SpaceException e) {
        return Result.build(null, ResultCodeEnum.SPACE_IS_SMALLER.getCode(),"空间不足","error");

    }
    @ExceptionHandler(value = ShareException.class)
    public Result ShareException(ShareException e) {
        return Result.build(null, ResultCodeEnum.SHARE_ERROR.getCode(),"分享链接不存在或已失效","error");

    }
    @ExceptionHandler(value = ShareCodeException.class)
    public Result ShareCodeException(ShareCodeException e) {
        return Result.build(null, ResultCodeEnum.SHARE_CODE_ERROR.getCode(),"分享码已失效，请重新输入","error");

    }
    @ExceptionHandler(value = MyException.class)
    public Result MyException(MyException e) {
        if(e instanceof ParamException){
            return Result.build(null, ResultCodeEnum.PARAM_ERROR.getCode(),"参数错误","error");
        }
        if(e instanceof LoginOutException){
            return Result.build(null, ResultCodeEnum.LOGIN_TIMEOUT.getCode(),"登陆超时","error");
        }
        if(e instanceof PathException){
            return Result.build(null, ResultCodeEnum.PATH_NOT_FOUND.getCode(),"路径错误","error");
        }
        if(e instanceof SpaceException){
            return Result.build(null, ResultCodeEnum.SPACE_IS_SMALLER.getCode(),"空间不足","error");
        }
        if(e instanceof ShareException){
            return Result.build(null, ResultCodeEnum.SHARE_ERROR.getCode(),"分享链接不存在或已失效","error");

        }
        if(e instanceof ShareCodeException){
            return Result.build(null, ResultCodeEnum.SHARE_CODE_ERROR.getCode(),"分享码已失效，请重新输入","error");
        }
        return  Result.build(null, ResultCodeEnum.ERROR.getCode(),e.getMessage(),"error");
    }
}
