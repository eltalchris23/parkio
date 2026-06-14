CREATE TABLE estacionamiento (
                                 id BIGSERIAL PRIMARY KEY,
                                 nombre VARCHAR(150) NOT NULL,
                                 descripcion VARCHAR(500),
                                 latitud NUMERIC(10,8) NOT NULL,
                                 longitud NUMERIC(11,8) NOT NULL,
                                 activo BOOLEAN NOT NULL DEFAULT TRUE,
                                 fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 fecha_actualizacion TIMESTAMP
);
