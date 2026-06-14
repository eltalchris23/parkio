package com.kasaca.parkio.usuario.repository;

import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {
}
