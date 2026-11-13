package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import ca.bc.gov.open.digitalformsapi.viirp.model.ErrorMessage;

@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = HttpStatus.valueOf(status.value()).getReasonPhrase();
        if (ex instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .distinct().sorted().collect(Collectors.joining("; "));
            if (message.isEmpty()) {
                message = "Request validation failed.";
            }
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "Malformed or missing request body.";
        }
        if (status.is5xxServerError()) {
            log.error("Request processing failed", ex);
            message = "Internal Server Error";
        } else {
            log.warn("Request rejected: {}", message);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new ErrorMessage(message), responseHeaders, status);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> validationException(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request validation failed.");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> badRequestException(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorMessage> multipartException(MultipartException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed multipart request.");
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorMessage> conflictException(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorMessage> resourceNotFoundException(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorMessage> unauthorizedException(UnauthorizedException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorMessage> forbiddenException(ForbiddenException ex) {
        return error(HttpStatus.FORBIDDEN, "Forbidden");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> globalExceptionHandler(Exception ex) {
        log.error("Request processing failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessage("Internal Server Error"));
    }

    private ResponseEntity<ErrorMessage> error(HttpStatus status, String message) {
        log.warn("Request rejected ({}): {}", status.value(), message);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ErrorMessage(message));
    }
}
