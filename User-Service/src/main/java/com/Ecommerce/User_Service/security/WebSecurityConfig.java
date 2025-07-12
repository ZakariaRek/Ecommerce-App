package com.Ecommerce.User_Service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.Ecommerce.User_Service.security.jwt.AuthEntryPointJwt;
import com.Ecommerce.User_Service.security.jwt.AuthTokenFilter;
import com.Ecommerce.User_Service.security.services.UserDetailsServiceImpl;
import com.Ecommerce.User_Service.security.oAuth2.OAuth2AuthenticationSuccessHandler;
import com.Ecommerce.User_Service.security.oAuth2.OAuth2AuthenticationFailureHandler;

import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class  WebSecurityConfig {

  @Autowired
  UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Autowired
  private CorsConfigurationSource corsConfigurationSource;

  @Autowired
  private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

  @Autowired
  private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                    auth
                            .requestMatchers("/auth/**").permitAll()
                            .requestMatchers("/api/users/auth/**").permitAll()
                            .requestMatchers("/api/users/actuator/**").permitAll()
                            .requestMatchers("/api/test/**").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()
                            .requestMatchers("/swagger-ui.html").permitAll()
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/swagger-resources/**").permitAll()
                            .requestMatchers("/webjars/**").permitAll()
                            .requestMatchers("/api/users/api/auth/**").permitAll()
                            .requestMatchers("/oauth2/**").permitAll()
                            .requestMatchers("/login/oauth2/**").permitAll()

                            // OAuth2 endpoints
                            .requestMatchers("/api/users/oauth2/**").permitAll()
                            .requestMatchers("/login/oauth2/**").permitAll()
                            .anyRequest().authenticated()
            )
            // OAuth2 Login configuration
//            .oauth2Login(oauth2 -> oauth2
//                    .authorizationEndpoint(authorization -> authorization
//                            .baseUri("/oauth2/authorize")
//
//                    )
//                    .redirectionEndpoint(redirection -> redirection
//                            .baseUri("/oauth2/callback/*")
//                    )
//                    .successHandler(oAuth2AuthenticationSuccessHandler)
//                    .failureHandler(oAuth2AuthenticationFailureHandler)
//            );
             .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorize")  // This will be at http://localhost:8081/oauth2/authorize
            )
            .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*")  // This will be at http://localhost:8081/oauth2/callback/*
            )
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
    );


    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}