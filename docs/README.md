# Documentación Parkio

## ERD
- `erd/parkio-erd.puml`

## UML
- `uml/parkio-domain.puml`
- `uml/parkio-use-cases.puml`

## Arquitectura
- `architecture/spring-boot-architecture.puml`
- `architecture/parkio-package-structure.puml`
- `architecture/parkio-jwt-flow.puml`

## Secuencia
- `sequence/parkio-login-sequence.puml`
- `sequence/parkio-create-estacionamiento-sequence.puml`
- `sequence/parkio-create-cajon-sequence.puml`

## API
- `api/parkio-api-v1.md`

## Casos de uso
- `use-cases/mvp-use-cases.md`

## Estado actual

Los módulos Rol y Estacionamiento cuentan con CRUD REST, validaciones, servicios transaccionales, mappers y pruebas unitarias. El manejo global de excepciones está implementado. Los flujos de autenticación, Usuario y Cajón documentados en esta carpeta continúan como arquitectura objetivo.
