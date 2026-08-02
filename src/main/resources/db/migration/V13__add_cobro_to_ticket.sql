ALTER TABLE ticket
    ADD COLUMN minutos_estancia INTEGER,
    ADD COLUMN monto_total NUMERIC(10, 2),
    ADD COLUMN precio_por_hora_aplicado NUMERIC(10, 2),
    ADD COLUMN minutos_tolerancia_aplicados INTEGER,
    ADD COLUMN cobrar_fraccion_aplicado BOOLEAN,
    ADD COLUMN tarifa_minima_aplicada NUMERIC(10, 2);

ALTER TABLE ticket
    ADD CONSTRAINT chk_ticket_minutos_estancia
        CHECK (minutos_estancia IS NULL OR minutos_estancia >= 0),
    ADD CONSTRAINT chk_ticket_monto_total
        CHECK (monto_total IS NULL OR monto_total >= 0),
    ADD CONSTRAINT chk_ticket_precio_por_hora_aplicado
        CHECK (precio_por_hora_aplicado IS NULL OR precio_por_hora_aplicado >= 0),
    ADD CONSTRAINT chk_ticket_minutos_tolerancia_aplicados
        CHECK (minutos_tolerancia_aplicados IS NULL OR minutos_tolerancia_aplicados >= 0),
    ADD CONSTRAINT chk_ticket_tarifa_minima_aplicada
        CHECK (tarifa_minima_aplicada IS NULL OR tarifa_minima_aplicada >= 0);
