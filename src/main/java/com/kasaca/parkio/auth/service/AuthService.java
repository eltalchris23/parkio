package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Contrato del modulo de autenticacion.
 *
 * <p>Define las operaciones relacionadas con validacion de credenciales,
 * emision de tokens JWT y consulta del usuario autenticado.</p>
 */
public interface AuthService {

    /**
     * Valida las credenciales de un usuario y genera un token JWT cuando son correctas.
     *
     * @param request credenciales recibidas desde el cliente
     * @return respuesta con token JWT y metadatos de expiracion
     */
    AuthResponse login(AuthLoginRequest request);

    /**
     * Consulta la informacion actual del usuario autenticado usando el JWT validado.
     *
     * @param jwt token JWT validado previamente por Spring Security
     * @return datos publicos y actuales del usuario autenticado
     */
    UsuarioResponse getCurrentUser(Jwt jwt);
}
