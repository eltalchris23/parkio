package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface RolService {

    PageResponse<RolResponse> getRoles(Pageable pageable);

    RolResponse getRol(Long rolId);

    RolResponse addRol(RolRequest request);

    RolResponse updateRol(Long rolId, RolRequest request);

    void deleteRol(Long rolId);

}
