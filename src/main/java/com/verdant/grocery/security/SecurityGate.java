package com.verdant.grocery.security;

import com.verdant.grocery.common.ApiException;
import com.verdant.grocery.customers.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.IOException;
import java.util.Set;

/**
 * Server-side portal authorization for the independent ADMIN and CUSTOMER roles.
 * Blocking private HTML pages as well as REST calls prevents exposing an admin
 * screen just because a visitor knows its filename. UI navigation is not a guard.
 */
@Component
public class SecurityGate implements HandlerInterceptor, WebMvcConfigurer {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final CustomerService accounts;

    public SecurityGate(CustomerService accounts) { this.accounts = accounts; }

    private boolean validAccount(HttpSession session, String role) {
        if(session == null || session.getAttribute("customerId") == null || !role.equals(session.getAttribute("role"))) return false;
        try {
            var account=accounts.get(session.getAttribute("customerId").toString());
            return account.getActive() && role.equals(account.getRole());
        } catch(RuntimeException ex) {
            return false;
        }
    }

    private void enforce(HttpSession session, String role) {
        require(session, role);
        // A disabled/deleted account must lose access immediately even with
        // a previously valid session cookie.
        if(!validAccount(session, role)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again");
    }

    private static String require(HttpSession session, String expectedRole) {
        if (session == null || session.getAttribute("customerId") == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        if (!expectedRole.equals(session.getAttribute("role"))) {
            throw ApiException.denied();
        }
        return session.getAttribute("customerId").toString();
    }

    public static String user(HttpSession session) {
        return require(session, "CUSTOMER");
    }

    public static void admin(HttpSession session) {
        require(session, "ADMIN");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**", "/admin*.html", "/customer-portal.html");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        HttpSession session = request.getSession(false);
        if (path.endsWith(".html")) {
            if (path.equals("/admin-login.html")) return true;
            String role = path.equals("/customer-portal.html") ? "CUSTOMER" : "ADMIN";
            // For browser navigation, redirect to the corresponding login rather
            // than presenting a JSON error page to visitors with expired sessions.
            if (!validAccount(session, role)) {
                response.sendRedirect(role.equals("ADMIN") ? "/admin-login.html" : "/login.html");
                return false;
            }
            return true;
        }

        if (path.startsWith("/api/admin/")) enforce(session, "ADMIN");
        else if (path.startsWith("/api/customer/") || path.startsWith("/api/account/")
                || path.equals("/api/cart") || path.startsWith("/api/cart/")
                || path.startsWith("/api/checkout/") || path.equals("/api/orders")
                || path.startsWith("/api/orders/") || path.startsWith("/api/deliveries/")
                || path.startsWith("/api/payments/") || path.equals("/api/feedback")
                || path.startsWith("/api/feedback/")) enforce(session, "CUSTOMER");

        // A custom header makes accidental cross-site form submission unsuitable
        // for state-changing requests. Actual access control remains role-based.
        if (!SAFE_METHODS.contains(request.getMethod())
                && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Request confirmation header missing");
        }
        return true;
    }
}
