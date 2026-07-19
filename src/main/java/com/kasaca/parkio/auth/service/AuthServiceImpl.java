package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.security.jwt.JwtService;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.shared.exception.UnauthorizedException;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.mapper.UsuarioMapper;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion del servicio de autenticacion.
 *
 * <p>Valida las credenciales contra los usuarios persistidos, delega la
 * generacion del token JWT al servicio especializado de seguridad y permite
 * consultar el usuario autenticado actual.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
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

    /**
     * Consulta el usuario autenticado a partir del JWT validado por Spring Security.
     *
     * <p>Extrae el claim usuarioId del token, consulta la base de datos y devuelve
     * la informacion vigente del usuario. Esto evita que el frontend dependa de
     * decodificar el JWT o de usar datos que pudieron quedar desactualizados.</p>
     */
    @Override
    public UsuarioResponse getCurrentUser(Jwt jwt) {
        Long usuarioId = extractUsuarioId(jwt);

        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        return usuarioMapper.toResponse(usuario);
    }

    /**
     * Extrae el identificador del usuario desde el claim usuarioId del JWT.
     *
     * <p>Si el token no contiene el claim esperado o el valor no es numerico,
     * se responde como token invalido para no trabajar con una identidad incompleta.</p>
     */
    private Long extractUsuarioId(Jwt jwt) {
        Object claim = jwt.getClaim("usuarioId");

        if (claim instanceof Number usuarioId) {
            return usuarioId.longValue();
        }

        throw new UnauthorizedException("Token invalido");
    }
}
