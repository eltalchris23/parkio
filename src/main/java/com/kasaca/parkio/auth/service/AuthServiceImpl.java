package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.security.jwt.JwtService;
import com.kasaca.parkio.shared.exception.UnauthorizedException;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion del servicio de autenticacion.
 *
 * <p>Valida las credenciales contra los usuarios persistidos y delega la
 * generacion del token JWT al servicio especializado de seguridad.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Realiza el inicio de sesion de un usuario.
     *
     * <p>Busca al usuario activo por correo, verifica la contrasena usando BCrypt
     * y, si las credenciales son validas, genera un token JWT con la identidad y
     * roles del usuario. Los usuarios desactivados por borrado logico no pueden
     * iniciar sesion.</p>
     */
    @Override
    public AuthResponse login(AuthLoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales invalidas");
        }

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}
