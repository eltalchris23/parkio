# Parkio API v1

## Información General

### Base URL

```text
/api
```

### Autenticación

La autenticación JWT está implementada mediante Spring Security y OAuth2 Resource Server.

Los endpoints públicos actuales son:

```http
POST /api/auth/login
POST /api/usuarios
```

Los demás endpoints requieren un token JWT válido en el encabezado `Authorization`.

La autorización granular por roles ya inició. Actualmente el módulo Rol requiere rol `ADMIN`, el módulo Usuario distingue entre operaciones administrativas de `ADMIN` y operaciones propias de `USER` u `OPERADOR`, el módulo Estacionamiento permite consulta a `ADMIN`, `OPERADOR` y `USER`, dejando la escritura únicamente a `ADMIN`, y el módulo Cajón permite consulta a `ADMIN`, `OPERADOR` y `USER`, cambio de estado a `ADMIN` y `OPERADOR`, y escritura administrativa solo a `ADMIN`.

Los roles incluidos en el claim `roles` del JWT se convierten en authorities de Spring Security con prefijo `ROLE_`. Por ejemplo, `ADMIN` se interpreta como `ROLE_ADMIN`.

### Borrado lógico

Las operaciones `DELETE` de Rol, Usuario, Estacionamiento y Cajón realizan borrado lógico mediante `activo=false`.

Reglas de comportamiento:

- Los registros inactivos no se devuelven en listados.
- Consultar un registro inactivo por identificador responde `404 Not Found`.
- Un usuario inactivo no puede iniciar sesión.
- Al desactivar un estacionamiento, también se desactivan sus cajones activos.
- Las restricciones únicas de base de datos siguen aplicando aunque el registro esté inactivo.

### Bootstrap del primer ADMIN

El endpoint público `POST /api/usuarios` crea usuarios con el rol base `USER`. Por seguridad, no existe un endpoint público para crear administradores.

Para habilitar el primer administrador en un entorno local o controlado:

```sql
SELECT id, email
FROM usuario
WHERE email = 'tu-correo@dominio.com';

SELECT id, nombre
FROM rol
WHERE nombre = 'ADMIN';

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.nombre = 'ADMIN'
WHERE u.email = 'tu-correo@dominio.com'
ON CONFLICT DO NOTHING;
```

Después de asignar el rol, el usuario debe iniciar sesión nuevamente para obtener un JWT actualizado con `ADMIN` dentro del claim `roles`.

### Estado de implementación

| Módulo | Estado |
|---|---|
| Rol | CRUD REST implementado y protegido con rol `ADMIN` |
| Auth | Login JWT implementado |
| Usuario | CRUD REST, roles, estacionamientos, validaciones, hash BCrypt y autorización `ADMIN`/`USER`/`OPERADOR` implementados |
| Estacionamiento | CRUD REST implementado con autorización `ADMIN`/`OPERADOR`/`USER` para consulta y `ADMIN` para escritura |
| Cajón | CRUD REST, validaciones y autorización `ADMIN`/`OPERADOR`/`USER` implementados |

Los endpoints protegidos deben enviar el token con este formato:

Ejemplo:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

# Módulo Auth

## Login

### Endpoint

```http
POST /api/auth/login
```

### Request

```json
{
  "email": "admin@parkio.com",
  "password": "123456"
}
```

### Response 200

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Response 401

```json
{
  "timestamp": "2026-07-07T09:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales invalidas",
  "path": "/api/auth/login",
  "validationErrors": {}
}
```

---

# Módulo Rol

Seguridad:

- Requiere JWT válido.
- Requiere rol `ADMIN`.
- Si no se envía token, responde `401 Unauthorized`.
- Si el token es válido pero no contiene `ADMIN` en el claim `roles`, responde `403 Forbidden`.

## Listar Roles

```http
GET /api/roles
```

### Response 200

```json
[
  {
    "id": 1,
    "nombre": "ADMIN",
    "activo": true,
    "fechaCreacion": "2026-06-20T12:00:00"
  }
]
```

## Consultar Rol

```http
GET /api/roles/{rolId}
```

### Response 200

```json
{
  "id": 1,
  "nombre": "ADMIN",
  "activo": true,
  "fechaCreacion": "2026-06-20T12:00:00"
}
```

### Response 404

Se devuelve cuando el identificador no corresponde a un rol existente.

## Crear Rol

```http
POST /api/roles
```

### Request

```json
{
  "nombre": "ADMIN",
  "activo": true
}
```

Validaciones:

- `nombre` es obligatorio y admite hasta 50 caracteres.
- `activo` es obligatorio.
- El nombre no puede estar duplicado.

### Response 201

```json
{
  "id": 1,
  "nombre": "ADMIN",
  "activo": true,
  "fechaCreacion": "2026-06-20T12:00:00"
}
```

## Actualizar Rol

```http
PUT /api/roles/{rolId}
```

Utiliza el mismo cuerpo y las mismas validaciones de la creación.

### Response 200

Devuelve el rol actualizado.

## Eliminar Rol

```http
DELETE /api/roles/{rolId}
```

### Response 204

La eliminación es lógica mediante `activo=false` y la respuesta no contiene cuerpo.

---

# Módulo Usuario

El CRUD de Usuario y la asignación de roles y estacionamientos están implementados bajo `/api/usuarios`. Las contraseñas se transforman en hashes BCrypt y nunca se incluyen en las respuestas.

Seguridad actual:

- Requiere JWT válido en todos sus endpoints.
- `POST /api/usuarios` es público para permitir el registro inicial y asigna automáticamente el rol base `USER`.
- `GET /api/usuarios`, `DELETE /api/usuarios/{id}` y asignaciones/retiros de roles o estacionamientos requieren `ADMIN`.
- `GET /api/usuarios/{id}`, `PUT /api/usuarios/{id}` y `PATCH /api/usuarios/{id}/password` permiten `ADMIN`, o `USER`/`OPERADOR` cuando el `id` de la ruta coincide con el claim `usuarioId` del JWT.

La creación utiliza `UsuarioCreateRequest`, la actualización general utiliza `UsuarioUpdateRequest` y el cambio de contraseña utiliza `UsuarioPasswordRequest`.

## Listar Usuarios

### Endpoint

```http
GET /api/usuarios
```

### Response 200

```json
[
  {
    "id": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@parkio.com",
    "activo": true,
    "fechaCreacion": "2026-06-28T12:00:00",
    "roles": [],
    "estacionamientoIds": []
  }
]
```

---

## Crear Usuario

### Endpoint

```http
POST /api/usuarios
```

### Request

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com",
  "password": "123456"
}
```

### Response 201

```json
{
  "id": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com",
  "activo": true,
  "fechaCreacion": "2026-06-28T12:00:00",
  "roles": [
    "USER"
  ],
  "estacionamientoIds": []
}
```

La creación pública agrega automáticamente el rol base `USER`, siempre que exista en la tabla `rol`.

### Respuestas de error

- `400 Bad Request`: datos inválidos.
- `404 Not Found`: el rol base `USER` no existe.
- `409 Conflict`: el correo ya está registrado.

---

## Consultar Usuario

### Endpoint

```http
GET /api/usuarios/{id}
```

### Response 200

```json
{
  "id": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com",
  "activo": true,
  "fechaCreacion": "2026-06-28T12:00:00",
  "roles": [],
  "estacionamientoIds": []
}
```

### Respuestas de error

- `404 Not Found`: el usuario no existe.

---

## Actualizar Usuario

### Endpoint

```http
PUT /api/usuarios/{id}
```

### Request

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@parkio.com"
}
```

### Response 200

Devuelve un `UsuarioResponse` con la misma estructura documentada en la consulta.

### Respuestas de error

- `400 Bad Request`: datos inválidos.
- `404 Not Found`: el usuario no existe.
- `409 Conflict`: el correo pertenece a otro usuario.

---

## Cambiar Contraseña de Usuario

### Endpoint

```http
PATCH /api/usuarios/{id}/password
```

### Request

```json
{
  "nuevaPassword": "nueva-clave"
}
```

### Response 204

Sin cuerpo de respuesta. La contraseña se almacena como hash BCrypt.

### Respuestas de error

- `400 Bad Request`: la nueva contraseña está vacía.
- `404 Not Found`: el usuario no existe.

---

## Eliminar Usuario

### Endpoint

```http
DELETE /api/usuarios/{id}
```

### Response 204

Sin cuerpo de respuesta. La eliminación es lógica mediante `activo=false`.

### Respuestas de error

- `404 Not Found`: el usuario no existe.

---

## Asignar Rol a Usuario

### Endpoint

```http
POST /api/usuarios/{usuarioId}/roles
```

### Request

```json
{
  "rolId": 1
}
```

### Response 200

Devuelve el `UsuarioResponse` actualizado, incluyendo el nombre del rol dentro de `roles`.

### Respuestas de error

- `400 Bad Request`: `rolId` es nulo o no es positivo.
- `404 Not Found`: el usuario o el rol no existe.
- `409 Conflict`: el usuario ya tiene asignado el rol.

---

## Retirar Rol de Usuario

### Endpoint

```http
DELETE /api/usuarios/{usuarioId}/roles/{rolId}
```

### Response 204

Sin cuerpo de respuesta.

### Respuestas de error

- `404 Not Found`: el usuario o el rol no existe.
- `409 Conflict`: el usuario no tiene asignado el rol.

---

## Asignar Estacionamiento a Usuario

### Endpoint

```http
POST /api/usuarios/{usuarioId}/estacionamientos
```

### Request

```json
{
  "estacionamientoId": 1
}
```

### Response 200

Devuelve el `UsuarioResponse` actualizado, incluyendo el identificador dentro de `estacionamientoIds`.

### Respuestas de error

- `400 Bad Request`: `estacionamientoId` es nulo o no es positivo.
- `404 Not Found`: el usuario o el estacionamiento no existe.
- `409 Conflict`: el usuario ya tiene asignado el estacionamiento.

---

## Retirar Estacionamiento de Usuario

### Endpoint

```http
DELETE /api/usuarios/{usuarioId}/estacionamientos/{estacionamientoId}
```

### Response 204

Sin cuerpo de respuesta.

### Respuestas de error

- `404 Not Found`: el usuario o el estacionamiento no existe.
- `409 Conflict`: el usuario no tiene asignado el estacionamiento.

---

# Módulo Estacionamiento

Seguridad actual:

- Requiere JWT válido en todos sus endpoints.
- `GET /api/estacionamientos` y `GET /api/estacionamientos/{estacionamientoId}` permiten `ADMIN`, `OPERADOR` y `USER`.
- `POST /api/estacionamientos`, `PUT /api/estacionamientos/{estacionamientoId}` y `DELETE /api/estacionamientos/{estacionamientoId}` requieren `ADMIN`.

## Listar Estacionamientos

### Endpoint

```http
GET /api/estacionamientos
```

### Response 200

```json
[
  {
    "id": 1,
    "nombre": "Parkio Centro",
    "descripcion": "Sucursal Centro Histórico",
    "latitud": 19.432608,
    "longitud": -99.133209,
    "activo": true,
    "fechaCreacion": "2026-06-21T12:00:00"
  }
]
```

## Consultar Estacionamiento

### Endpoint

```http
GET /api/estacionamientos/{estacionamientoId}
```

### Response 200

```json
{
  "id": 1,
  "nombre": "Parkio Centro",
  "descripcion": "Sucursal Centro Histórico",
  "latitud": 19.432608,
  "longitud": -99.133209,
  "activo": true,
  "fechaCreacion": "2026-06-21T12:00:00"
}
```

### Response 404

Se devuelve cuando el identificador no corresponde a un estacionamiento existente.

## Crear Estacionamiento

### Endpoint

```http
POST /api/estacionamientos
```

### Request

```json
{
  "nombre": "Parkio Centro",
  "descripcion": "Sucursal Centro Histórico",
  "latitud": 19.432608,
  "longitud": -99.133209
}
```

Validaciones:

- `nombre` es obligatorio y admite hasta 150 caracteres.
- `descripcion` es opcional y admite hasta 500 caracteres.
- `latitud` es obligatoria, admite hasta 8 decimales y debe estar entre `-90` y `90`.
- `longitud` es obligatoria, admite hasta 8 decimales y debe estar entre `-180` y `180`.

### Response 201

Devuelve el estacionamiento creado con el mismo formato de la consulta individual.

## Actualizar Estacionamiento

### Endpoint

```http
PUT /api/estacionamientos/{estacionamientoId}
```

Utiliza el mismo cuerpo y las mismas validaciones de la creación.

### Response 200

Devuelve el estacionamiento actualizado.

### Response 404

Se devuelve cuando el identificador no corresponde a un estacionamiento existente.

## Eliminar Estacionamiento

### Endpoint

```http
DELETE /api/estacionamientos/{estacionamientoId}
```

### Response 204

La eliminación es lógica mediante `activo=false`, también desactiva los cajones activos asociados y la respuesta no contiene cuerpo.

### Response 404

Se devuelve cuando el identificador no corresponde a un estacionamiento existente.

### Response 409

Se devuelve cuando una relación existente impide eliminar el estacionamiento.

---

# Módulo Cajón

Seguridad actual:

- Requiere JWT válido en todos sus endpoints.
- `GET /api/cajones`, `GET /api/cajones?estacionamientoId={id}` y `GET /api/cajones/{cajonId}` permiten `ADMIN`, `OPERADOR` y `USER`.
- `PATCH /api/cajones/{cajonId}/estado` permite `ADMIN` y `OPERADOR`.
- `POST /api/cajones`, `PUT /api/cajones/{cajonId}` y `DELETE /api/cajones/{cajonId}` requieren `ADMIN`.

## Listar Cajones

```http
GET /api/cajones
```

### Response 200

Devuelve todos los cajones registrados.

## Listar Cajones por Estacionamiento

```http
GET /api/cajones?estacionamientoId=1
```

### Response 200

```json
[
  {
    "id": 1,
    "numero": "A-001",
    "tipo": "AUTO",
    "estado": "LIBRE",
    "estacionamientoId": 1,
    "activo": true,
    "fechaCreacion": "2026-06-27T12:00:00"
  }
]
```

### Response 404

Se devuelve cuando el estacionamiento no existe.

## Consultar Cajón

```http
GET /api/cajones/{cajonId}
```

### Response 200

Devuelve el cajón solicitado con el formato anterior.

### Response 404

Se devuelve cuando el cajón no existe.

## Crear Cajón

### Endpoint

```http
POST /api/cajones
```

### Request

```json
{
  "numero": "A-001",
  "tipo": "AUTO",
  "estacionamientoId": 1
}
```

Tipos permitidos: `AUTO`, `MOTO`, `DISCAPACITADO` y `ELECTRICO`.

El estado inicial se asigna automáticamente como `LIBRE`. El número y tipo son obligatorios, y el identificador del estacionamiento debe ser un número positivo.

### Response 201

```json
{
  "id": 1,
  "estacionamientoId": 1,
  "numero": "A-001",
  "tipo": "AUTO",
  "estado": "LIBRE",
  "activo": true,
  "fechaCreacion": "2026-06-27T12:00:00"
}
```

### Response 404

Se devuelve cuando el estacionamiento no existe.

### Response 409

Se devuelve cuando el número ya existe dentro del estacionamiento.

## Actualizar Cajón

```http
PUT /api/cajones/{cajonId}
```

Utiliza el mismo cuerpo de la creación. La actualización conserva el estado actual del cajón.

### Response 200

Devuelve el cajón actualizado.

### Response 404

Se devuelve cuando el cajón o estacionamiento no existe.

### Response 409

Se devuelve cuando el número ya está utilizado por otro cajón del estacionamiento.

## Cambiar Estado del Cajón

```http
PATCH /api/cajones/{cajonId}/estado
```

### Request

```json
{
  "estado": "OCUPADO"
}
```

Estados permitidos: `LIBRE`, `OCUPADO` y `FUERA_SERVICIO`.

### Response 200

Devuelve el cajón con el estado actualizado.

### Response 400

Se devuelve cuando el estado es nulo o no corresponde a un valor permitido.

### Response 404

Se devuelve cuando el cajón no existe.

## Eliminar Cajón

```http
DELETE /api/cajones/{cajonId}
```

### Response 204

La eliminación es lógica mediante `activo=false` y la respuesta no contiene cuerpo.

### Response 404

Se devuelve cuando el cajón no existe.

---

# Códigos HTTP utilizados

| Código | Descripción           |
| ------ | --------------------- |
| 200    | Operación exitosa     |
| 201    | Recurso creado        |
| 204    | Operación exitosa sin cuerpo |
| 400    | Datos inválidos       |
| 401    | No autenticado        |
| 403    | Sin permisos          |
| 404    | Recurso no encontrado |
| 409    | Conflicto             |
| 500    | Error interno         |

## Formato de Error Implementado

Las operaciones implementadas utilizan el siguiente formato:

```json
{
  "timestamp": "2026-06-20T16:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos",
  "path": "/api/roles",
  "validationErrors": {
    "nombre": "El nombre del rol es obligatorio"
  }
}
```
