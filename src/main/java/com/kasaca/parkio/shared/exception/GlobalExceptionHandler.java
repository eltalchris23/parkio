package com.kasaca.parkio.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la API REST de Parkio.
 *
 * <p>Convierte excepciones de negocio, validación y sistema en respuestas HTTP
 * consistentes representadas mediante {@link ApiError}.</p>
 *
 * <p>Respuestas administradas:</p>
 *
 * <ul>
 *     <li>400 Bad Request: datos inválidos o JSON mal formado.</li>
 *     <li>401 Unauthorized: credenciales invalidas o autenticacion requerida.</li>
 *     <li>404 Not Found: recurso solicitado inexistente.</li>
 *     <li>409 Conflict: conflicto de negocio o integridad de datos.</li>
 *     <li>500 Internal Server Error: error inesperado.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja solicitudes sobre recursos que no existen.
     *
     * @return respuesta HTTP 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja conflictos relacionados con reglas de negocio.
     *
     * <p>Por ejemplo, intentar registrar un rol con un nombre existente.</p>
     *
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja errores de autenticacion controlados por la aplicacion.
     *
     * <p>Se utiliza principalmente cuando el login recibe credenciales
     * invalidas.</p>
     *
     * @return respuesta HTTP 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja errores de autorizacion cuando el usuario esta autenticado,
     * pero no cuenta con el rol o permiso requerido para ejecutar la operacion.
     *
     * @return respuesta HTTP 403 Forbidden
     */
    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<ApiError> handleAccessDenied(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "No tienes permisos suficientes para realizar esta operacion",
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja errores producidos por Jakarta Validation en los DTOs.
     *
     * <p>La respuesta incluye los mensajes de validación organizados por
     * nombre de campo.</p>
     *
     * @return respuesta HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Valor inválido"
                                : fieldError.getDefaultMessage(),
                        (firstMessage, ignoredMessage) -> firstMessage,
                        LinkedHashMap::new
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                validationErrors
        );
    }

    /**
     * Maneja cuerpos JSON vacíos, mal formados o incompatibles con el DTO.
     *
     * @return respuesta HTTP 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido",
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja violaciones de restricciones de la base de datos.
     *
     * <p>Por ejemplo, valores duplicados, referencias inválidas o intentos
     * de eliminar registros que todavía están relacionados.</p>
     *
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Conflicto de integridad de datos en {}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de datos",
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Maneja cualquier excepción inesperada que no tenga un tratamiento
     * más específico.
     *
     * <p>La excepción completa se registra internamente, pero no se incluyen
     * detalles técnicos en la respuesta enviada al cliente.</p>
     *
     * @return respuesta HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Error no controlado en {}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno inesperado",
                request.getRequestURI(),
                Map.of()
        );
    }

    /**
     * Construye el formato estándar de error utilizado por la API.
     *
     * @param status estado HTTP que devolverá la respuesta
     * @param message mensaje seguro para el consumidor de la API
     * @param path ruta donde ocurrió el error
     * @param validationErrors errores de validación organizados por campo
     * @return respuesta HTTP con el estado y cuerpo de error indicados
     */
    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
}
