package cl.luxury.catalogo.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorApi> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorApi> solicitudInvalida(SolicitudInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> cuerpoInvalido(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorApi> violacion(ConstraintViolationException e) {
        String detalle = e.getConstraintViolations().stream()
                .map((ConstraintViolation<?> v) -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorApi> formatoInvalido(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud mal formada o con un valor de tipo incorrecto.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos.");
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorApi(estado.value(), estado.getReasonPhrase(), mensaje));
    }
}
