package com.luomiblog.config;

import com.luomiblog.security.JwtAuthenticationFilter;
import com.luomiblog.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtConfig jwtConfig;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Swagger/OpenAPI 匿名放行开关（app.swagger.permit-all，默认 true 维持现状；
     * 生产建议 SWAGGER_PERMIT_ALL=false 关闭，避免 API 文档裸奔）
     */
    @Value("${app.swagger.permit-all:true}")
    private boolean swaggerPermitAll;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/site/**").permitAll()
                        // 安装向导全流程放行（首次部署需匿名完成安装）；
                        // verify-reinstall/reinstall 另有 @PreAuthorize("hasRole('ADMIN')") 方法级控制
                        .requestMatchers("/api/install/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/license/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll();
                    // Swagger 放行加属性开关（默认放行维持现状；当前无 springdoc 依赖，
                    // 此 matcher 为哑弹，补依赖后由该开关决定是否放行）
                    if (swaggerPermitAll) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/articles").hasAnyRole("BLOGGER", "ADMIN")
                        // 浏览量记录与点赞切换：设计上支持匿名访客（visitorId 维度），
                        // 登录用户由 JwtAuthenticationFilter 的 principal 注入身份
                        .requestMatchers(HttpMethod.POST, "/api/articles/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/articles/*/like").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyRole("BLOGGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasAnyRole("BLOGGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .anyRequest().authenticated();
                })
                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
