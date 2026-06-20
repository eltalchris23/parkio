package com.kasaca.parkio.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String recurso, Object identificador) {
        super("%s con identificador '%s' no fue encontrado"
                .formatted(recurso, identificador));
    }
}
