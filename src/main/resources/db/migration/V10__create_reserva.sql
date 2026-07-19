CREATE TABLE reserva (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(30) NOT NULL,
    placa VARCHAR(15),

    estado VARCHAR(30) NOT NULL,

    fecha_reserva TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    tiempo_expiracion_minutos INTEGER NOT NULL,

    usuario_id BIGINT NOT NULL,
    estacionamiento_id BIGINT NOT NULL,
    cajon_id BIGINT NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,

    CONSTRAINT uk_reserva_codigo UNIQUE (codigo),

    CONSTRAINT ck_reserva_estado
        CHECK (estado IN ('CREADA', 'CANCELADA', 'EXPIRADA', 'USADA')),

    CONSTRAINT ck_reserva_tiempo_expiracion
        CHECK (tiempo_expiracion_minutos > 0),

    CONSTRAINT ck_reserva_fechas
        CHECK (fecha_expiracion > fecha_reserva),

    CONSTRAINT fk_reserva_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id),

    CONSTRAINT fk_reserva_estacionamiento
        FOREIGN KEY (estacionamiento_id)
        REFERENCES estacionamiento (id),

    CONSTRAINT fk_reserva_cajon
        FOREIGN KEY (cajon_id)
        REFERENCES cajon (id)
);

CREATE INDEX idx_reserva_usuario_id
    ON reserva (usuario_id);

CREATE INDEX idx_reserva_estacionamiento_id
    ON reserva (estacionamiento_id);

CREATE INDEX idx_reserva_cajon_id
    ON reserva (cajon_id);

CREATE INDEX idx_reserva_estado
    ON reserva (estado);

CREATE INDEX idx_reserva_fecha_expiracion
    ON reserva (fecha_expiracion);
