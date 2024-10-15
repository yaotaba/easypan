package com.easypan.aspect;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.exception.LoginOutException;
import com.easypan.exception.ParamException;
import com.easypan.exception.PathException;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.SessionWebUserDto;
import com.easypan.utils.StringTools;
import com.easypan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class GlobalOperationAspect {
    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Pointcut("@annotation(com.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint Point) {
        try {
            Object target = Point.getTarget();//获取执行目标方法的实例对象
            Object[] arguments = Point.getArgs();//获取传递给目标方法的参数列表
            /*
            获取当前被拦截方法的签名信息，返回一个 Signature 对象。
            常见的 Signature 子接口：
            MethodSignature: 获取有关方法的详细信息。
            getMethod()：获取具体的 Method 对象。
            getParameterTypes()：获取方法参数的类型数组。
            FieldSignature: 获取字段的签名信息。
            ConstructorSignature: 获取构造函数的签名信息。
            例：calculateSum(int, int)：第一个方法的签名

             */
            String methodName = Point.getSignature().getName();//Point.getSignature()获取签名信息，返回方法名字
            Class<?>[] parameterTypes = ((MethodSignature) Point.getSignature()).getMethod().getParameterTypes();//获取参数类型
            Method method = target.getClass().getMethod(methodName, parameterTypes);//根据目标对象的类，方法名，参数列表的类型获取方法
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);//获取方法上的注解对象
            if (interceptor == null) {
                return;
            }
            //校验参数
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }
            //校验登录
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
        } catch (ParamException e) {//方法返回的异常在这里捕获
            logger.error("拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("拦截器异常", e);
            throw new RuntimeException(e);
        } catch (Throwable e) {
            logger.error("拦截器异常", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 校验登录和超级管理员
     *
     * @param checkAdmin
     */
    private void checkLogin(boolean checkAdmin) {
        //在类中获取request
        /*
        RequestContextHolder 提供了静态方法 getRequestAttributes()，
        让开发者能够在任意位置获取到当前线程的请求属性，从而可以访问和操作这些属性。
         */
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if (webUserDto == null) {//没有登录
            throw new LoginOutException("登陆超时");
        }
        if (checkAdmin && !webUserDto.getAdmin()) {//不是超级管理员像访问超级管理员
            throw new PathException("请求路径错误");
        }
    }

    /**
     * 校验参数
     *
     * @param m
     * @param arguments
     */
    private void validateParams(Method m, Object[] arguments) {
        Parameter[] parameters = m.getParameters();//获取所有参数信息
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];//获取第一个参数信息
            Object value = arguments[i];//获取参数的值
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);//获取verifyParam注解
            if (verifyParam == null) {
                continue;
            }
            //基本数据类型
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) || TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            } else {
                //传递对象
                checkObjectValue(parameter, value);
            }
        }
    }

    /**
     * 校验对象参数
     *
     * @param parameter
     * @param value
     */
    private void checkObjectValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class clazz = Class.forName(typeName);
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultvalue = field.get(value);
                checkValue(resultvalue, fieldVerifyParam);
            }
        } catch (ParamException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new ParamException("请求参数错误");
        }
    }

    /**
     * 校验基本数据类型参数
     *
     * @param value
     * @param verifyParam
     */
    private void checkValue(Object value, VerifyParam verifyParam) {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        /**
         * 校验空
         */
        if (isEmpty && verifyParam.required()) {
            throw new ParamException("请求参数错误");
        }
        /**
         * 校验长度
         */
        if (!isEmpty && (verifyParam.max() != -1 && length > verifyParam.max() || verifyParam.min() != -1 && length < verifyParam.min())) {
            throw new ParamException("请求参数错误");
        }
        /**
         * 校验正则
         */
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regx().getRegex()) && !VerifyUtils.verify(verifyParam.regx(), String.valueOf(value))) {
            throw new ParamException("请求参数错误");
        }
    }


}
