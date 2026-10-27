package com.healthcare.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.app.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionTokenManager sessionTokenManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole methodRole = handlerMethod.getMethodAnnotation(RequiresRole.class);
        RequiresRole classRole = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        RequiresRole targetAnnotation = methodRole != null ? methodRole : classRole;

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request.getParameter("token") != null) {
            // Support token query parameter for direct PDF downloads/previews
            token = request.getParameter("token");
        }

        Optional<SessionTokenManager.UserSession> sessionOpt = sessionTokenManager.getSession(token);

        if (sessionOpt.isPresent()) {
            SessionTokenManager.UserSession session = sessionOpt.get();
            request.setAttribute("USER_SESSION", session);

            if (targetAnnotation != null && targetAnnotation.value().length > 0) {
                boolean hasRole = Arrays.asList(targetAnnotation.value()).contains(session.getRole());
                if (!hasRole) {
                    sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Insufficient privileges for role " + session.getRole());
                    return false;
                }
            }
            return true;
        }

        if (targetAnnotation != null) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
            return false;
        }

        return true;
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
