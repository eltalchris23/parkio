package com.kasaca.parkio.rol.repository;

import com.kasaca.parkio.rol.entity.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre,Long id);

    Page<Rol> findByActivoTrue(Pageable pageable);

    Optional<Rol> findByIdAndActivoTrue(Long id);

    Optional<Rol> findByNombreAndActivoTrue(String nombre);
}
