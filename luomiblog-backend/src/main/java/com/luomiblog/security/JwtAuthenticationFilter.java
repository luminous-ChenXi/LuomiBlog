package com.luomiblog.security;

import com.luomiblog.service.MemoryCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 滑动续期新令牌的响应头（CORS 侧已同步暴露） */
    public static final String NEW_TOKEN_HEADER = "X-New-Token";

    private final JwtUtil jwtUtil;
    private final MemoryCacheService memoryCacheService;
    private final UserDetailsService userDetailsService;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String tokenId = jwtUtil.getTokenId(token);

            if (isTokenBlacklisted(tokenId)) {
                log.debug("Token is blacklisted: {}", tokenId);
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtUtil.isRefreshToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);

                // principal 必须是 UserPrincipal（而非用户名字符串）：
                // 全项目所有 @AuthenticationPrincipal UserPrincipal 参数都依赖它，
                // 否则恒为 null（点赞落库 user_id=NULL、收藏 NPE、评论身份丢失等）。
                // 角色与权限改为按 DB 实时加载：封禁/降权立即生效，优于令牌内快照。
                UserDetails principal;
                try {
                    principal = userDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    log.debug("令牌对应用户不存在或已停用: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 封禁/停用立即生效（与登录侧判定一致），不再等令牌自然过期
                if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
                    log.debug("令牌对应用户已封禁或停用: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 辰汐会话令牌滑动续期：活跃使用且剩余寿命不足一半时，
                // 通过 X-New-Token 响应头下发新令牌，前端负责替换本地存储
                if (jwtUtil.shouldSlide(token)) {
                    response.setHeader(NEW_TOKEN_HEADER, jwtUtil.slideToken(token));
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isTokenBlacklisted(String tokenId) {
        return memoryCacheService.exists(TOKEN_BLACKLIST_PREFIX + tokenId);
    }
}
