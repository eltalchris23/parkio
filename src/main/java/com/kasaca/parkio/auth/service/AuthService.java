package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;

/**
 * Contrato del modulo de autenticacion.
 *
 * <p>Define las operaciones relacionadas con validacion de credenciales
 * y emision de tokens JWT.</p>
 */
public interface AuthService {

    /**
     * Valida las credenciales de un usuario y genera un token JWT cuando son correctas.
     *
     * @param request credenciales recibidas desde el cliente
     * @return respuesta con token JWT y metadatos de expiracion
     */
    AuthResponse login(AuthLoginRequest request);
}
