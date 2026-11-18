package com.zhike.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhike.log.mapper.OperLogMapper;
import com.zhike.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面。只记「写操作」里标了 @LogOperation 的方法，
 * 不记查询——查询量大且噪声高，真要看访问情况应该用网关日志或 APM。
 * 日志写入失败不能影响主流程，所以整体 try-catch 兜住。
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    private final OperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    public OperLogAspect(OperLogMapper operLogMapper, ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint pjp, LogOperation annotation) throws Throwable {
        long begin = System.currentTimeMillis();
        boolean success = true;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            try {
                OperLog logRow = new OperLog();
                logRow.setModule(annotation.module());
                logRow.setAction(annotation.action());
                logRow.setMethod(pjp.getSignature().toShortString());
                logRow.setCostMs((int) (System.currentTimeMillis() - begin));
                logRow.setSuccess(success ? 1 : 0);
                logRow.setUsername(currentUsername());
                logRow.setParams(serializeArgs(pjp.getArgs()));
                HttpServletRequest req = currentRequest();
                if (req != null) {
                    logRow.setIp(clientIp(req));
                }
                operLogMapper.insert(logRow);
            } catch (Exception e) {
                log.warn("操作日志写入失败：{}", e.getMessage());
            }
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser u) {
            return u.getUsername();
        }
        return "anonymous";
    }

    private String serializeArgs(Object[] args) {
        try {
            String json = objectMapper.writeValueAsString(Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .toList());
            return json.length() > 900 ? json.substring(0, 900) : json;
        } catch (Exception e) {
            return "";
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim(); // 走代理时取第一个真实客户端地址
        }
        return req.getRemoteAddr();
    }
}
