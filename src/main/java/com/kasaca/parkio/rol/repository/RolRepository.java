package com.kasaca.parkio.rol.repository;

import com.kasaca.parkio.rol.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre,Long id);

    List<Rol> findByActivoTrue();

    Optional<Rol> findByIdAndActivoTrue(Long id);

    Optional<Rol> findByNombreAndActivoTrue(String nombre);
}
