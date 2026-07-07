package com.kasaca.parkio.usuario.repository;

import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Busca un usuario por correo electronico para procesos de autenticacion.
     *
     * @param email correo electronico registrado
     * @return usuario encontrado o vacio cuando no existe
     */
    Optional<Usuario> findByEmail(String email);
}
