package com.next.common;

import com.next.exception.BusinessException;
import com.next.exception.ParamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = RuntimeException.class)
    @ResponseBody
    public JsonData exceptionHandler(RuntimeException ex) {
        log.error("unknown exception", ex);
        if (ex instanceof ParamException || ex instanceof BusinessException) {
            return JsonData.fail(ex.getMessage());
        }
        return JsonData.fail("系统异常，请稍后尝试");
    }

    @ExceptionHandler(value = Error.class)
    @ResponseBody
    public JsonData errorHandler(Error ex) {
        log.error("unknown error", ex);
        return JsonData.fail("系统异常，请联系管理员");
    }
}
