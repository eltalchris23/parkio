CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    placa VARCHAR(15),

    fecha_entrada TIMESTAMP NOT NULL,
    fecha_salida TIMESTAMP,

    reserva_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    operador_entrada_id BIGINT NOT NULL,
    estacionamiento_id BIGINT NOT NULL,
    cajon_id BIGINT NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,

    CONSTRAINT uk_ticket_codigo UNIQUE (codigo),
    CONSTRAINT uk_ticket_reserva_id UNIQUE (reserva_id),

    CONSTRAINT ck_ticket_estado
        CHECK (estado IN ('ABIERTO', 'CERRADO')),

    CONSTRAINT ck_ticket_fechas
        CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada),

    CONSTRAINT fk_ticket_reserva
        FOREIGN KEY (reserva_id)
        REFERENCES reserva (id),

    CONSTRAINT fk_ticket_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id),

    CONSTRAINT fk_ticket_operador_entrada
        FOREIGN KEY (operador_entrada_id)
        REFERENCES usuario (id),

    CONSTRAINT fk_ticket_estacionamiento
        FOREIGN KEY (estacionamiento_id)
        REFERENCES estacionamiento (id),

    CONSTRAINT fk_ticket_cajon
        FOREIGN KEY (cajon_id)
        REFERENCES cajon (id)
);

CREATE INDEX idx_ticket_usuario_id
    ON ticket (usuario_id);

CREATE INDEX idx_ticket_operador_entrada_id
    ON ticket (operador_entrada_id);

CREATE INDEX idx_ticket_estacionamiento_id
    ON ticket (estacionamiento_id);

CREATE INDEX idx_ticket_cajon_id
    ON ticket (cajon_id);

CREATE INDEX idx_ticket_estado
    ON ticket (estado);

CREATE INDEX idx_ticket_fecha_entrada
    ON ticket (fecha_entrada);
