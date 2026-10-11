package com.deb.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    @Pointcut("within(@org.springframework.stereotype.Service *) && !@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void servicePointcut() {}

    @Pointcut("execution(* com.deb.repository..*(..))")
    public void repositoryPointcut() {}

    @Around("controllerPointcut()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround(joinPoint, "ENDPOINT");
    }

    @Around("servicePointcut()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround(joinPoint, "SERVICE");
    }

    @Around("repositoryPointcut()")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround(joinPoint, "DAO");
    }

    private Object logAround(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[{}] Enter: {}.{}() with args = {}", layer, className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            log.info("[{}] Exit: {}.{}() executed in {}ms", layer, className, methodName, executionTime);
            return result;
        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("[{}] Failed: {}.{}() after {}ms with error: {}",
                    layer, className, methodName, executionTime, ex.getMessage());
            throw ex;
        }
    }
}
