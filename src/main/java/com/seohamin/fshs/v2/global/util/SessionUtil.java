package com.seohamin.fshs.v2.global.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SessionUtil {

    /**
     * 로그인 된 유저 강제 로그아웃 시키는 메서드
     * @param request HttpServletRequest
     */
    public void forceLogout(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
