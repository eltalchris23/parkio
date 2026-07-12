package com.kasaca.parkio.usuario.repository;

import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Usuario> findByActivoTrue(Pageable pageable);

    Optional<Usuario> findByIdAndActivoTrue(Long id);

    Optional<Usuario> findByEmailAndActivoTrue(String email);
}
