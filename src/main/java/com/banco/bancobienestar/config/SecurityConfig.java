package com.banco.bancobienestar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

     @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }//metodo para encriptar la contraseña

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
        .authorizeHttpRequests(authz -> authz
            //recursos publicos
            .requestMatchers("/css/**","/js/**","/img/**").permitAll()
            //rutas permitidas para clientes y ejecutivos
            .requestMatchers("/dashboard","/tranferencias","/procesar-transferencia","/credito","/procesar-credito","/api/v1/finanzas/**").authenticated()
            //panel del admin por rol ejecutivo
            .requestMatchers("/admin/**").hasRole("EJECUTIVO").anyRequest().authenticated()
        )
        .formLogin(form -> form
            //Especificamos la vista del login personalizado
            .loginPage("/login")
            .defaultSuccessUrl("/dashboard",true)
            .failureUrl("/login?error=true")
            .permitAll()
        )
            .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout=true")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );
        return http.build();
    }
   
}
