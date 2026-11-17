package ca.bc.gov.open.digitalformsapi.viirp.exception;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import tools.jackson.databind.json.JsonMapper;

public final class ApiProblemDetails {

    private ApiProblemDetails() { }

    public record ValidationError(String field, String message) { }

    public static ProblemDetail create(HttpServletRequest request, HttpStatusCode status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        // Keep the original path on error dispatches, but never include query parameters or the host.
        String path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String original
                ? original : request.getRequestURI();
        problem.setInstance(URI.create(path));
        return problem;
    }

    public static ProblemDetail validation(HttpServletRequest request, List<ValidationError> errors) {
        ProblemDetail problem = create(request, HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setProperty("errors", errors.stream().distinct()
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
                .toList());
        return problem;
    }

    public static void write(JsonMapper mapper, HttpServletResponse response, ProblemDetail problem)
            throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
