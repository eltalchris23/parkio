CREATE TABLE cajon (
                       id BIGSERIAL PRIMARY KEY,
                       estacionamiento_id BIGINT NOT NULL,
                       numero VARCHAR(20) NOT NULL,
                       tipo VARCHAR(30) NOT NULL,
                       estado VARCHAR(30) NOT NULL,
                       activo BOOLEAN NOT NULL DEFAULT TRUE,
                       fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       fecha_actualizacion TIMESTAMP,

                       CONSTRAINT fk_cajon_estacionamiento
                           FOREIGN KEY (estacionamiento_id)
                               REFERENCES estacionamiento(id),

                       CONSTRAINT uk_cajon_numero
                           UNIQUE (estacionamiento_id, numero)
);
