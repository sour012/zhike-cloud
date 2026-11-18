package com.zhike.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 每个请求解析一次 Bearer 令牌。解析不出来就清空上下文继续往下走，
 * 最终由 SecurityConfig 里配置的 entryPoint 统一返回 401，避免在过滤器里写两套响应格式。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;
    private final JwtProperties props;

    public JwtAuthenticationFilter(JwtTokenProvider provider, JwtProperties props) {
        this.provider = provider;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(props.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(props.getPrefix())) {
            String token = header.substring(props.getPrefix().length()).trim();
            LoginUser user = provider.parse(token);
            if (user != null) {
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
