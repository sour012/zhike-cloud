package com.zhike.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。原则：可预期的错误（业务校验、参数、权限）给出明确中文提示；
 * 意料之外的异常记日志、对外只给一句话，不泄漏内部实现。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        // 业务码直接映射成 HTTP 状态：前端拦截器按状态区分「未登录 / 无权限 / 普通业务错误」
        HttpStatus status = switch (e.getCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @Valid 校验失败：只回第一个字段错误，前端直接弹这句提示即可 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "请求参数不合法" : fe.getField() + " " + fe.getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.fail(400, msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleDenied(AccessDeniedException e) {
        return Result.fail(403, "没有权限执行该操作");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuth(AuthenticationException e) {
        return Result.fail(401, "登录状态已失效，请重新登录");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e, HttpServletRequest req) {
        log.error("未处理异常 {} {}", req.getMethod(), req.getRequestURI(), e);
        return Result.fail(500, "服务内部错误，请稍后重试");
    }
}
