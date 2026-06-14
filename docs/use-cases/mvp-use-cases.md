Casos de Uso Detallados
## CU-001 Iniciar Sesión

Actor

ADMIN
PROPIETARIO
OPERADOR

Precondiciones

Usuario registrado.
Usuario activo.

Flujo Principal

Ingresa email.
Ingresa contraseña.
El sistema valida credenciales.
El sistema genera JWT.
El sistema devuelve token.

Resultado

Usuario autenticado.

## CU-002 Crear Estacionamiento

Actor

ADMIN

Precondiciones

Usuario autenticado.
Rol ADMIN.

Flujo Principal

Captura nombre.
Captura descripción.
Captura latitud.
Captura longitud.
Guarda información.

Resultado

Estacionamiento registrado.

## CU-003 Asignar Usuario a Estacionamiento

Actor

ADMIN

Flujo Principal

Selecciona usuario.
Selecciona estacionamiento.
Guarda relación.

Resultado

Usuario asignado.

## CU-004 Crear Cajón

Actor

ADMIN
PROPIETARIO

Flujo Principal

Selecciona estacionamiento.
Captura número.
Captura tipo.
Guarda cajón.

Resultado

Cajón disponible para operación.

## CU-005 Consultar Cajones

Actor

ADMIN
PROPIETARIO
OPERADOR

Resultado

Visualiza los cajones y su estado.