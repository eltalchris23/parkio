package com.kasaca.parkio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Registra el codificador BCrypt que utilizarán los servicios para generar
     * y verificar hashes seguros sin almacenar contraseñas en texto plano.
     *
     * @return codificador de contraseñas administrado por Spring
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
