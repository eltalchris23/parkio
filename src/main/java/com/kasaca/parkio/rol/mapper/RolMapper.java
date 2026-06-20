package com.kasaca.parkio.rol.mapper;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.entity.Rol;
import org.springframework.stereotype.Component;

@Component
public class RolMapper {

    public Rol toEntity(RolRequest request) {
        Rol rol = new Rol();
        updateEntity(request, rol);
        return rol;
    }

    public void updateEntity(RolRequest request, Rol rol) {
        rol.setNombre(request.nombre());
        rol.setActivo(request.activo());
    }

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getActivo(),
                rol.getFechaCreacion()
        );
    }
}
