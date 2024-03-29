package se.iths.springbootgroupproject.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.client.RestClient;
import se.iths.springbootgroupproject.services.github.GithubOAuth2UserService;

@Configuration
public class SecurityConfig {

    @Autowired
    private GithubOAuth2UserService githubOAuth2UserService;

    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/web/welcome", "/login", "/oauth/**", "/logout", "/error**").permitAll()
                        .requestMatchers("/web/hello").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint
                                        .userService(githubOAuth2UserService))
                        .successHandler(oauth2LoginSuccessHandler()));
        return http.build();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER\n" +
                               "ROLE_USER > ROLE_GUEST");
        return hierarchy;
    }

    @Bean
    public AuthenticationSuccessHandler oauth2LoginSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler("/web/users/profiles/demo");
    }

    @Bean
    RestClient restClient() {
        return RestClient.create();
    }
}
