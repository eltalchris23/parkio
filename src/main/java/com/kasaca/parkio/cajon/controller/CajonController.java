package com.kasaca.parkio.cajon.controller;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.service.CajonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cajones")
@RequiredArgsConstructor
public class CajonController {

    private final CajonService cajonService;

    @GetMapping
    public List<CajonResponse> getCajones(@RequestParam(required = false) Long estacionamientoId) {
        if (estacionamientoId == null) {
            return cajonService.getCajones();
        }

        return cajonService.getCajonesByEstacionamientoId(
                estacionamientoId
        );
    }

    @GetMapping("/{cajonId}")
    public CajonResponse getCajon(@PathVariable Long cajonId) {
        return cajonService.getCajon(cajonId);
    }

    @PostMapping
    public ResponseEntity<CajonResponse> addCajon(@Valid @RequestBody CajonRequest request) {
        CajonResponse response = cajonService.addCajon(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{cajonId}")
    public CajonResponse updateCajon(@PathVariable Long cajonId,@Valid @RequestBody CajonRequest request) {
        return cajonService.updateCajon(
                cajonId,
                request
        );
    }

    @PatchMapping("/{cajonId}/estado")
    public CajonResponse updateEstado(
            @PathVariable Long cajonId,
            @Valid @RequestBody CajonEstadoRequest request
    ) {
        return cajonService.updateEstado(cajonId, request);
    }

    @DeleteMapping("/{cajonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCajon(@PathVariable Long cajonId) {
        cajonService.deleteCajon(cajonId);
    }
}
