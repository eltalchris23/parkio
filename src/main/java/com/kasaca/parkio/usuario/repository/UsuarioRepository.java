package com.kasaca.parkio.usuario.repository;

import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Usuario> findByActivoTrue(Pageable pageable);

    Optional<Usuario> findByIdAndActivoTrue(Long id);

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    /**
     * Verifica si un usuario activo tiene asignado un rol activo por nombre.
     *
     * <p>Se utiliza desde reglas de autorizacion para confirmar que un usuario
     * objetivo realmente sea OPERADOR antes de permitir acciones limitadas a OWNER.</p>
     */
    @Query("""
            SELECT COUNT(usuario) > 0
            FROM Usuario usuario
            JOIN usuario.roles rol
            WHERE usuario.id = :usuarioId
              AND usuario.activo = TRUE
              AND rol.nombre = :rolNombre
              AND rol.activo = TRUE
            """)
    boolean existsUsuarioActivoConRol(
            @Param("usuarioId") Long usuarioId,
            @Param("rolNombre") String rolNombre
    );

    /**
     * Verifica si un operador activo esta asignado a por lo menos un estacionamiento
     * activo cuyo owner sea el usuario autenticado.
     *
     * <p>Sirve para permitir que OWNER consulte o actualice operadores bajo su
     * propio alcance sin darle permisos globales sobre todos los usuarios.</p>
     */
    @Query("""
            SELECT COUNT(usuario) > 0
            FROM Usuario usuario
            JOIN usuario.roles rol
            JOIN usuario.estacionamientos estacionamiento
            WHERE usuario.id = :operadorId
              AND usuario.activo = TRUE
              AND rol.nombre = 'OPERADOR'
              AND rol.activo = TRUE
              AND estacionamiento.activo = TRUE
              AND estacionamiento.owner.id = :ownerId
              AND estacionamiento.owner.activo = TRUE
            """)
    boolean existsOperadorActivoAsignadoAEstacionamientoDeOwner(
            @Param("operadorId") Long operadorId,
            @Param("ownerId") Long ownerId
    );

    /**
     * Verifica si un operador activo esta asignado a un estacionamiento especifico
     * que pertenece al owner autenticado.
     *
     * <p>Se usa para retirar asociaciones de forma segura, evitando que OWNER
     * modifique relaciones de estacionamientos que no le pertenecen.</p>
     */
    @Query("""
            SELECT COUNT(usuario) > 0
            FROM Usuario usuario
            JOIN usuario.roles rol
            JOIN usuario.estacionamientos estacionamiento
            WHERE usuario.id = :operadorId
              AND usuario.activo = TRUE
              AND rol.nombre = 'OPERADOR'
              AND rol.activo = TRUE
              AND estacionamiento.id = :estacionamientoId
              AND estacionamiento.activo = TRUE
              AND estacionamiento.owner.id = :ownerId
              AND estacionamiento.owner.activo = TRUE
            """)
    boolean existsOperadorActivoAsignadoAEstacionamientoPropio(
            @Param("operadorId") Long operadorId,
            @Param("ownerId") Long ownerId,
            @Param("estacionamientoId") Long estacionamientoId
    );
}
