package com.showdown.backend.config;

import com.showdown.backend.repository.AppUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@ConditionalOnProperty(name = "showdown.security.mode", havingValue = "database", matchIfMissing = true)
public class DatabaseUserDetailsConfig {
    @Bean
    UserDetailsService databaseUserDetailsService(AppUserRepository users) {
        return username -> {
            var user = users.findByEmailAndActiveTrue(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
            String[] authorities = user.getUserRoles().stream()
                    .map(userRole -> userRole.getRole().getCode().toUpperCase())
                    .map(code -> "ROLE_" + code)
                    .toArray(String[]::new);
            return User.withUsername(user.getEmail()).password(user.getPasswordHash())
                    .authorities(authorities).disabled(!Boolean.TRUE.equals(user.getActive())).build();
        };
    }
}
