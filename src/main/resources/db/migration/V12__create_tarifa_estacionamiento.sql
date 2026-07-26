CREATE TABLE tarifa_estacionamiento (
    id BIGSERIAL PRIMARY KEY,

    estacionamiento_id BIGINT NOT NULL,

    precio_por_hora NUMERIC(10, 2) NOT NULL,

    minutos_tolerancia INTEGER NOT NULL DEFAULT 0,

    cobrar_fraccion BOOLEAN NOT NULL DEFAULT TRUE,

    tarifa_minima NUMERIC(10, 2) NOT NULL DEFAULT 0,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_actualizacion TIMESTAMP,

    CONSTRAINT fk_tarifa_estacionamiento_estacionamiento
        FOREIGN KEY (estacionamiento_id)
        REFERENCES estacionamiento (id),

    CONSTRAINT uk_tarifa_estacionamiento_estacionamiento
        UNIQUE (estacionamiento_id),

    CONSTRAINT chk_tarifa_estacionamiento_precio_por_hora
        CHECK (precio_por_hora >= 0),

    CONSTRAINT chk_tarifa_estacionamiento_minutos_tolerancia
        CHECK (minutos_tolerancia >= 0),

    CONSTRAINT chk_tarifa_estacionamiento_tarifa_minima
        CHECK (tarifa_minima >= 0)
);
