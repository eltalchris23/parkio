package com.kasaca.parkio.catalogo.controller;

import com.kasaca.parkio.catalogo.dto.CatalogoResponse;
import com.kasaca.parkio.catalogo.service.CatalogoService;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock
    private CatalogoService catalogoService;

    private MockMvc mockMvc;

    /**
     * Configura MockMvc con el controlador de catalogos y el manejador global de errores.
     */
    @BeforeEach
    void setUp() {
        CatalogoController controller = new CatalogoController(catalogoService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Verifica que el endpoint de tipos de cajon responda con ApiResponse.
     */
    @Test
    void debeConsultarTiposDeCajon() throws Exception {
        when(catalogoService.getTiposCajon()).thenReturn(List.of(
                new CatalogoResponse("AUTO", "Auto"),
                new CatalogoResponse("MOTO", "Moto")
        ));

        mockMvc.perform(get("/catalogos/cajones/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Tipos de cajon consultados correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].codigo").value("AUTO"))
                .andExpect(jsonPath("$.data[0].descripcion").value("Auto"))
                .andExpect(jsonPath("$.data[1].codigo").value("MOTO"))
                .andExpect(jsonPath("$.data[1].descripcion").value("Moto"));

        verify(catalogoService).getTiposCajon();
    }

    /**
     * Verifica que el endpoint de estados de cajon responda con ApiResponse.
     */
    @Test
    void debeConsultarEstadosDeCajon() throws Exception {
        when(catalogoService.getEstadosCajon()).thenReturn(List.of(
                new CatalogoResponse("LIBRE", "Libre"),
                new CatalogoResponse("FUERA_SERVICIO", "Fuera de servicio")
        ));

        mockMvc.perform(get("/catalogos/cajones/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Estados de cajon consultados correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].codigo").value("LIBRE"))
                .andExpect(jsonPath("$.data[0].descripcion").value("Libre"))
                .andExpect(jsonPath("$.data[1].codigo").value("FUERA_SERVICIO"))
                .andExpect(jsonPath("$.data[1].descripcion").value("Fuera de servicio"));

        verify(catalogoService).getEstadosCajon();
    }
}
