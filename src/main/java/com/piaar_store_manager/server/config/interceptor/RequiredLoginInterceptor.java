package com.piaar_store_manager.server.config.interceptor;

import com.piaar_store_manager.server.annotation.RequiredLogin;
import com.piaar_store_manager.server.exception.CustomInvalidUserException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequiredLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod == false) {
            return false;
        }

        HandlerMethod method = (HandlerMethod) handler;

        /**
         * RequiredLogin requiredLogin = method.getMethodAnnotation(RequiredLogin.class); => 메서드 단에서 처리
         * 만약에
         * RequiredLogin requiredLogin = method.getBean().getClass().getAnnotation(RequiredLogin.class);
         * 에서 버그 발생시 메서드 단에서 처리하는 부분으로 다시 복구
         */
        RequiredLogin requiredLogin = method.getMethodAnnotation(RequiredLogin.class);

        if(requiredLogin == null) {
            requiredLogin = method.getBean().getClass().getAnnotation(RequiredLogin.class);
        }

        if(requiredLogin == null) {
            return true;
        }

        if(SecurityContextHolder.getContext().getAuthentication().getName() != null &&
                SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            throw new CustomInvalidUserException("로그인이 필요한 서비스입니다.");
        }else {
            return true;
        }
    }
}
