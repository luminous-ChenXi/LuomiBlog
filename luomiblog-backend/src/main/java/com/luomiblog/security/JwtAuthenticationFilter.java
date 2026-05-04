package com.luomiblog.security;

import com.luomiblog.service.MemoryCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemoryCacheService memoryCacheService;

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
                String roleCode = jwtUtil.getRoleFromToken(token);
                List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roleCode != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
                }
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
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
