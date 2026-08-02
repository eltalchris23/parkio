ALTER TABLE ticket
    DROP CONSTRAINT ck_ticket_estado;

ALTER TABLE ticket
    ADD CONSTRAINT ck_ticket_estado
        CHECK (estado IN ('ABIERTO', 'PENDIENTE_PAGO', 'CERRADO'));
