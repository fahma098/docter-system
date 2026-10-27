package com.healthcare.app.security;

import com.healthcare.app.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static SessionTokenManager.UserSession getCurrentSession() {
        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs instanceof ServletRequestAttributes servletAttribs) {
            HttpServletRequest request = servletAttribs.getRequest();
            Object sessionObj = request.getAttribute("USER_SESSION");
            if (sessionObj instanceof SessionTokenManager.UserSession userSession) {
                return userSession;
            }
        }
        throw new UnauthorizedException("No active authenticated session found");
    }

    public static Long getCurrentUserId() {
        return getCurrentSession().getUserId();
    }

    public static String getCurrentUserRole() {
        return getCurrentSession().getRole();
    }

    public static Long getCurrentPatientId() {
        Long pid = getCurrentSession().getPatientId();
        if (pid == null) {
            throw new UnauthorizedException("Current user is not registered as a patient");
        }
        return pid;
    }

    public static Long getCurrentDoctorId() {
        Long did = getCurrentSession().getDoctorId();
        if (did == null) {
            throw new UnauthorizedException("Current user is not registered as a doctor");
        }
        return did;
    }
}
