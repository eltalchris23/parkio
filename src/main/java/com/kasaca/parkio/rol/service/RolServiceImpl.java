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

    @Override
    public List<RolResponse> getRoles() {
        return rolRepository.findAll()
                .stream()
                .map(rolMapper::toResponse)
                .toList();
    }

    @Override
    public RolResponse getRol(Long rolId) {
        return rolMapper.toResponse(findRolById(rolId));
    }

    @Override
    @Transactional
    public RolResponse addRol(RolRequest request) {
        validateUniqueName(request.nombre());

        Rol rol = rolMapper.toEntity(request);
        Rol savedRol = rolRepository.save(rol);

        return rolMapper.toResponse(savedRol);
    }

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

    @Override
    @Transactional
    public void deleteRol(Long rolId) {
        Rol rol = findRolById(rolId);
        rolRepository.delete(rol);
    }

    private Rol findRolById(Long rolId) {
        return rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));
    }

    private void validateUniqueName(String nombre) {
        if (rolRepository.existsByNombre(nombre)) {
            throw new ConflictException(
                    "Ya existe un rol con el nombre '%s'".formatted(nombre)
            );
        }
    }
}
