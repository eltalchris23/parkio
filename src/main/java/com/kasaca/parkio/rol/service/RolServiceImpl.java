package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.rol.mapper.RolMapper;
import com.kasaca.parkio.rol.repository.RolRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    /**
     * Obtiene únicamente los roles activos para no exponer registros desactivados
     * mediante borrado lógico.
     */
    @Override
    public List<RolResponse> getRoles() {
        return rolRepository.findByActivoTrue()
                .stream()
                .map(rolMapper::toResponse)
                .toList();
    }

    /**
     * Consulta un rol activo por identificador.
     */
    @Override
    public RolResponse getRol(Long rolId) {
        return rolMapper.toResponse(findRolById(rolId));
    }

    /**
     * Crea un rol nuevo después de validar que su nombre no esté registrado.
     */
    @Override
    @Transactional
    public RolResponse addRol(RolRequest request) {
        validateUniqueName(request.nombre());

        Rol rol = rolMapper.toEntity(request);
        Rol savedRol = rolRepository.save(rol);

        return rolMapper.toResponse(savedRol);
    }

    /**
     * Actualiza un rol activo después de validar que el nuevo nombre no esté duplicado.
     */
    @Override
    @Transactional
    public RolResponse updateRol(Long rolId, RolRequest request) {
        Rol rol = findRolById(rolId);

        if (rolRepository.existsByNombreAndIdNot(request.nombre(), rolId)) {
            throw new ConflictException(
                    "Ya existe un rol con el nombre '%s'".formatted(request.nombre())
            );
        }

        rolMapper.updateEntity(request, rol);
        Rol updatedRol = rolRepository.save(rol);

        return rolMapper.toResponse(updatedRol);
    }

    /**
     * Realiza el borrado lógico del rol cambiando su bandera activo a false.
     * No elimina físicamente el registro para conservar trazabilidad.
     */
    @Override
    @Transactional
    public void deleteRol(Long rolId) {
        Rol rol = findRolById(rolId);

        rol.setActivo(false);
        rolRepository.save(rol);
    }

    /**
     * Busca un rol activo por identificador o lanza una excepción 404 cuando no existe
     * o cuando fue desactivado mediante borrado lógico.
     */
    private Rol findRolById(Long rolId) {
        return rolRepository.findByIdAndActivoTrue(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));
    }

    /**
     * Verifica que no exista otro rol con el mismo nombre antes de crear uno nuevo.
     */
    private void validateUniqueName(String nombre) {
        if (rolRepository.existsByNombre(nombre)) {
            throw new ConflictException(
                    "Ya existe un rol con el nombre '%s'".formatted(nombre)
            );
        }
    }
}
