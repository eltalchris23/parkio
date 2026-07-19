package com.kasaca.parkio.catalogo.service;

import com.kasaca.parkio.catalogo.dto.CatalogoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoServiceImplTest {

    private CatalogoServiceImpl catalogoService;

    /**
     * Prepara el servicio de catalogos antes de cada prueba.
     */
    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoServiceImpl();
    }

    /**
     * Verifica que los tipos de cajon expuestos correspondan al enum TipoCajon.
     */
    @Test
    void debeConsultarTiposDeCajon() {
        List<CatalogoResponse> response = catalogoService.getTiposCajon();

        assertThat(response)
                .extracting(CatalogoResponse::codigo)
                .containsExactly("AUTO", "MOTO", "DISCAPACITADO", "ELECTRICO");

        assertThat(response)
                .extracting(CatalogoResponse::descripcion)
                .containsExactly("Auto", "Moto", "Discapacitado", "Electrico");
    }

    /**
     * Verifica que los estados de cajon expuestos correspondan al enum EstadoCajon.
     */
    @Test
    void debeConsultarEstadosDeCajon() {
        List<CatalogoResponse> response = catalogoService.getEstadosCajon();

        assertThat(response)
                .extracting(CatalogoResponse::codigo)
                .containsExactly("LIBRE", "OCUPADO", "FUERA_SERVICIO");

        assertThat(response)
                .extracting(CatalogoResponse::descripcion)
                .containsExactly("Libre", "Ocupado", "Fuera de servicio");
    }
}
