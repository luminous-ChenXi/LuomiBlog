package com.luomiblog.config;

import com.luomiblog.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:4321,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // 安装接口本身放行，已安装时的写操作由 InstallController/InstallServiceImpl 内部校验，
                        // verify-reinstall/reinstall 另有 @PreAuthorize("hasRole('ADMIN')") 方法级控制
                        .requestMatchers("/api/install/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        // 授权状态横幅（N2 占位，公开只读，绝不影响功能可用性）
                        .requestMatchers(HttpMethod.GET, "/api/license/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/site/**").permitAll()
                        // 文章公开读接口（仅 GET，写操作必须认证 + 角色）
                        .requestMatchers(HttpMethod.GET,
                                "/api/articles",
                                "/api/articles/search",
                                "/api/articles/category/{categoryId}",
                                "/api/articles/id/{id}",
                                "/api/articles/{slug}",
                                "/api/articles/{articleId}/stats",
                                "/api/articles/{articleId}/check").permitAll()
                        // 浏览/点赞/收藏统计（匿名可上报，归属不信任请求体）
                        .requestMatchers(HttpMethod.POST,
                                "/api/articles/{articleId}/view",
                                "/api/articles/{articleId}/like",
                                "/api/articles/{articleId}/favorite").permitAll()
                        // 评论/分类/标签公开读
                        .requestMatchers(HttpMethod.GET, "/api/comments/article/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                        // 点赞状态查询（匿名可查）
                        .requestMatchers(HttpMethod.GET, "/api/likes/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 只允许配置的白名单来源（app.cors.allowed-origins），不允许 * 通配
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList()));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 让浏览器端跨域请求能读到辰汐会话滑动续期下发的新令牌
        configuration.setExposedHeaders(Arrays.asList(JwtAuthenticationFilter.NEW_TOKEN_HEADER));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
