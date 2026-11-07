package cn.iocoder.yudao.module.aigc.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

/** Adapt an HttpOnly media cookie to yudao's existing token filter, only for
 * authenticated read-only asset content. Never authenticate mutation routes by cookie. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MediaAuthorizationFilter extends OncePerRequestFilter {
    public static final String COOKIE="zaomeng_aigc_media";
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !request.getRequestURI().matches("/admin-api/aigc/assets/[0-9]+/content") || request.getHeader("Authorization")!=null;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        String token=null;
        if(request.getCookies()!=null)for(var cookie:request.getCookies())if(COOKIE.equals(cookie.getName()))token=cookie.getValue();
        if(token==null||!token.matches("[a-zA-Z0-9._-]{16,512}")){chain.doFilter(request,response);return;}
        final String bearer="Bearer "+token;
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public String getHeader(String name){return "Authorization".equalsIgnoreCase(name)?bearer:super.getHeader(name);}
            @Override public Enumeration<String> getHeaders(String name){return "Authorization".equalsIgnoreCase(name)?Collections.enumeration(List.of(bearer)):super.getHeaders(name);}
            @Override public Enumeration<String> getHeaderNames(){var names=Collections.list(super.getHeaderNames());names.add("Authorization");return Collections.enumeration(names);}
        },response);
    }
}
