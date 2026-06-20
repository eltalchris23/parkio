package com.kasaca.parkio.rol.repository;

import com.kasaca.parkio.rol.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre,Long id);
}
