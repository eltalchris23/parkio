CREATE TABLE usuario_estacionamiento (
                                         usuario_id BIGINT NOT NULL,
                                         estacionamiento_id BIGINT NOT NULL,

                                         PRIMARY KEY (usuario_id, estacionamiento_id),

                                         CONSTRAINT fk_ue_usuario
                                             FOREIGN KEY (usuario_id)
                                                 REFERENCES usuario(id),

                                         CONSTRAINT fk_ue_estacionamiento
                                             FOREIGN KEY (estacionamiento_id)
                                                 REFERENCES estacionamiento(id)
);
