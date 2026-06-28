# Parkio API v1

## Información General

### Base URL

```text
/api
```

### Autenticación

La autenticación JWT está planificada, pero todavía no está implementada. Los endpoints actuales no requieren token y no deben considerarse seguros para un entorno productivo.

### Estado de implementación

| Módulo | Estado |
|---|---|
| Rol | CRUD REST implementado |
| Auth | Propuesto; no implementado |
| Usuario | CRUD REST, roles, validaciones y hash BCrypt implementados |
| Estacionamiento | CRUD REST implementado |
| Cajón | CRUD REST y validaciones implementados |

El siguiente encabezado representa el formato de autenticación previsto para el futuro:

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
  "message": "Credenciales inválidas"
}
```

---

# Módulo Rol

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

La eliminación actual es física y la respuesta no contiene cuerpo.

---

# Módulo Usuario

El CRUD de Usuario y la asignación de roles están implementados bajo `/api/usuarios`. Las contraseñas se transforman en hashes BCrypt y nunca se incluyen en las respuestas.

Limitaciones actuales:

- No existe asignación de estacionamientos a usuarios.
- No existe autenticación ni JWT.
- Creación y actualización utilizan `UsuarioRequest`; por ello, actualizar exige enviar una contraseña y genera un hash nuevo.

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
    "roles": []
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
  "roles": []
}
```

### Respuestas de error

- `400 Bad Request`: datos inválidos.
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
  "roles": []
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
  "email": "juan@parkio.com",
  "password": "nueva-clave"
}
```

### Response 200

Devuelve un `UsuarioResponse` con la misma estructura documentada en la consulta.

### Respuestas de error

- `400 Bad Request`: datos inválidos.
- `404 Not Found`: el usuario no existe.
- `409 Conflict`: el correo pertenece a otro usuario.

---

## Eliminar Usuario

### Endpoint

```http
DELETE /api/usuarios/{id}
```

### Response 204

Sin cuerpo de respuesta.

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

# Módulo Estacionamiento

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

La eliminación actual es física y la respuesta no contiene cuerpo.

### Response 404

Se devuelve cuando el identificador no corresponde a un estacionamiento existente.

### Response 409

Se devuelve cuando una relación existente impide eliminar el estacionamiento.

---

# Módulo Cajón

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

La eliminación es física y la respuesta no contiene cuerpo.

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
