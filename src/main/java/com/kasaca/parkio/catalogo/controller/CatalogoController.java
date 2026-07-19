package com.kasaca.parkio.catalogo.controller;

import com.kasaca.parkio.catalogo.dto.CatalogoResponse;
import com.kasaca.parkio.catalogo.service.CatalogoService;
import com.kasaca.parkio.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/catalogos")
@Tag(
        name = "Catalogos",
        description = "Endpoints de consulta para catalogos simples usados por el frontend"
)
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {

    private final CatalogoService catalogoService;

    /**
     * Consulta los tipos de cajon disponibles.
     *
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con los tipos de cajon disponibles
     */
    @Operation(
            summary = "Consultar tipos de cajon",
            description = "Devuelve los valores disponibles del enum TipoCajon para que el frontend construya selects sin quemar valores."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tipos de cajon consultados correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar catalogos"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping(value = "/cajones/tipos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> getTiposCajon(
            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        List<CatalogoResponse> response = catalogoService.getTiposCajon();

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Tipos de cajon consultados correctamente",
                        response
                )
        );
    }

    /**
     * Consulta los estados de cajon disponibles.
     *
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con los estados de cajon disponibles
     */
    @Operation(
            summary = "Consultar estados de cajon",
            description = "Devuelve los valores disponibles del enum EstadoCajon para que el frontend construya selects sin quemar valores."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estados de cajon consultados correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar catalogos"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping(value = "/cajones/estados", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> getEstadosCajon(
            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        List<CatalogoResponse> response = catalogoService.getEstadosCajon();

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Estados de cajon consultados correctamente",
                        response
                )
        );
    }
}
