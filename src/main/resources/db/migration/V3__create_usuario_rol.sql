CREATE TABLE usuario_rol (
                             usuario_id BIGINT NOT NULL,
                             rol_id BIGINT NOT NULL,

                             PRIMARY KEY (usuario_id, rol_id),

                             CONSTRAINT fk_usuario_rol_usuario
                                 FOREIGN KEY (usuario_id)
                                     REFERENCES usuario(id),

                             CONSTRAINT fk_usuario_rol_rol
                                 FOREIGN KEY (rol_id)
                                     REFERENCES rol(id)
);
