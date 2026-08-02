CREATE TABLE pago (
    id BIGSERIAL PRIMARY KEY,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,

    ticket_id BIGINT NOT NULL,
    operador_id BIGINT NOT NULL,

    monto_total NUMERIC(10, 2) NOT NULL,
    monto_recibido NUMERIC(10, 2) NOT NULL,
    cambio NUMERIC(10, 2) NOT NULL,

    metodo_pago VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_pago TIMESTAMP NOT NULL,

    CONSTRAINT fk_pago_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES ticket (id),

    CONSTRAINT fk_pago_operador
        FOREIGN KEY (operador_id)
        REFERENCES usuario (id),

    CONSTRAINT uk_pago_ticket
        UNIQUE (ticket_id),

    CONSTRAINT chk_pago_monto_total
        CHECK (monto_total >= 0),

    CONSTRAINT chk_pago_monto_recibido
        CHECK (monto_recibido >= 0),

    CONSTRAINT chk_pago_cambio
        CHECK (cambio >= 0),

    CONSTRAINT chk_pago_monto_recibido_suficiente
        CHECK (monto_recibido >= monto_total),

    CONSTRAINT chk_pago_metodo
        CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),

    CONSTRAINT chk_pago_estado
        CHECK (estado IN ('REGISTRADO'))
);
