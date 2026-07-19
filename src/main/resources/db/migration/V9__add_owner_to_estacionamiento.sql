-- Agrega owner_id para identificar al usuario dueño del estacionamiento.
-- Se deja nullable inicialmente para no romper estacionamientos existentes sin dueño asignado.
ALTER TABLE estacionamiento
ADD COLUMN owner_id BIGINT;

-- Garantiza que el dueño del estacionamiento sea un usuario existente.
ALTER TABLE estacionamiento
ADD CONSTRAINT fk_estacionamiento_owner
FOREIGN KEY (owner_id)
REFERENCES usuario(id);

-- Optimiza las consultas de estacionamientos por dueño.
CREATE INDEX idx_estacionamiento_owner_id
ON estacionamiento(owner_id);
