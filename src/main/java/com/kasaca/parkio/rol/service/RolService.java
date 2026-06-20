package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;

import java.util.List;

public interface RolService {

    List<RolResponse> getRoles();

    RolResponse getRol(Long rolId);

    RolResponse addRol(RolRequest request);

    RolResponse updateRol(Long rolId, RolRequest request);

    void deleteRol(Long rolId);

}
