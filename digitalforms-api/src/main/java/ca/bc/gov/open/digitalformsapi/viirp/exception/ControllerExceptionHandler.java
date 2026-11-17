package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import ca.bc.gov.open.digitalformsapi.viirp.exception.ApiProblemDetails.ValidationError;

@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ProblemDetail problem = ApiProblemDetails.create(servletRequest, status,
                status.is5xxServerError() ? "Internal Server Error" : HttpStatus.valueOf(status.value()).getReasonPhrase());
        if (status.is5xxServerError()) {
            log.error("Request processing failed", ex);
        } else if (ex instanceof MethodArgumentNotValidException validation) {
            problem = ApiProblemDetails.validation(servletRequest, validation.getBindingResult().getAllErrors().stream()
                    .map(error -> validationError(error, "body")).toList());
        } else if (ex instanceof HandlerMethodValidationException validation) {
            List<ValidationError> errors = new ArrayList<>();
            validation.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                    errors.add(validationError(error, parameterName(result.getMethodParameter())))));
            validation.getCrossParameterValidationResults().forEach(error ->
                    errors.add(validationError(error, "body")));
            problem = ApiProblemDetails.validation(servletRequest, errors);
        } else if (ex instanceof HttpMessageNotReadableException) {
            problem = ApiProblemDetails.validation(servletRequest,
                    List.of(new ValidationError("body", "Malformed or missing request body.")));
        } else if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            problem = ApiProblemDetails.validation(servletRequest,
                    List.of(new ValidationError(mismatch.getName(), "Invalid value.")));
        } else if (ex instanceof MissingServletRequestParameterException missing) {
            problem = ApiProblemDetails.validation(servletRequest,
                    List.of(new ValidationError(missing.getParameterName(), "Required parameter is missing.")));
        } else if (ex instanceof MissingServletRequestPartException missing) {
            problem = ApiProblemDetails.validation(servletRequest,
                    List.of(new ValidationError(missing.getRequestPartName(), "Required part is missing.")));
        }
        if (!status.is5xxServerError()) {
            log.warn("Request rejected ({}): {}", status.value(), problem.getDetail());
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(ex, problem, responseHeaders, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> validationException(ConstraintViolationException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.validation(request, ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(validationPath(violation, request), violation.getMessage()))
                .toList()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> badRequestException(BadRequestException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.validation(request, List.of(new ValidationError(ex.getField(), ex.getMessage()))));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> multipartException(MultipartException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.validation(request,
                List.of(new ValidationError("body", "Malformed multipart request."))));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> conflictException(ConflictException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.create(request, HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> resourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.create(request, HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> unauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.create(request, HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> forbiddenException(ForbiddenException ex, HttpServletRequest request) {
        return error(ApiProblemDetails.create(request, HttpStatus.FORBIDDEN, "Forbidden"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> globalExceptionHandler(Exception ex, HttpServletRequest request) {
        log.error("Request processing failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiProblemDetails.create(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    }

    private ResponseEntity<ProblemDetail> error(ProblemDetail problem) {
        log.warn("Request rejected ({}): {}", problem.getStatus(), problem.getDetail());
        return ResponseEntity.status(problem.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    private static ValidationError validationError(MessageSourceResolvable error, String field) {
        return new ValidationError(error instanceof FieldError fieldError ? fieldError.getField() : field,
                error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage());
    }

    private static String parameterName(MethodParameter parameter) {
        for (var annotation : parameter.getParameterAnnotations()) {
            String name = switch (annotation) {
                case RequestParam value -> value.name().isEmpty() ? value.value() : value.name();
                case RequestHeader value -> value.name().isEmpty() ? value.value() : value.name();
                case RequestPart value -> value.name().isEmpty() ? value.value() : value.name();
                case PathVariable value -> value.name().isEmpty() ? value.value() : value.name();
                default -> "";
            };
            if (!name.isEmpty()) {
                return name;
            }
        }
        return "body";
    }

    private static String validationPath(ConstraintViolation<?> violation, HttpServletRequest request) {
        HandlerMethod handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)
                instanceof HandlerMethod method ? method : null;
        StringBuilder field = new StringBuilder();
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PARAMETER && handler != null) {
                MethodParameter parameter = handler.getMethodParameters()[node.as(Path.ParameterNode.class).getParameterIndex()];
                if (!parameter.hasParameterAnnotation(RequestBody.class)) {
                    field.append(parameterName(parameter));
                }
            } else if (node.getKind() == ElementKind.PROPERTY || node.getKind() == ElementKind.CONTAINER_ELEMENT) {
                if (node.isInIterable()) {
                    field.append('[').append(node.getIndex() == null ? "" : node.getIndex()).append(']');
                }
                if (node.getKind() == ElementKind.PROPERTY) {
                    if (!field.isEmpty()) {
                        field.append('.');
                    }
                    field.append(node.getName());
                }
            }
        }
        return field.isEmpty() ? "body" : field.toString();
    }
}
