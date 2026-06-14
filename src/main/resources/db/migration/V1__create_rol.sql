CREATE TABLE rol (
                     id BIGSERIAL PRIMARY KEY,
                     nombre VARCHAR(50) NOT NULL UNIQUE,
                     activo BOOLEAN NOT NULL DEFAULT TRUE,
                     fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     fecha_actualizacion TIMESTAMP
);
