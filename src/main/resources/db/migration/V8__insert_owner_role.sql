-- Inserta el rol base OWNER para representar al dueño de uno o varios estacionamientos.
-- ON CONFLICT evita que la migración falle si el rol ya existe en la tabla rol.
INSERT INTO rol (nombre, activo)
VALUES ('OWNER', TRUE)
ON CONFLICT (nombre) DO NOTHING;
