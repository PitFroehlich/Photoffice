package de.photoffice.web;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API-wide error responses (RFC 9457 problem details) for errors that are not specific to a module.
 * Module-specific errors are handled in the module's controller.
 */
@RestControllerAdvice
class ApiExceptionHandler {

	/** Constraint violations of query/path parameters (generated from the OpenAPI contract). */
	@ExceptionHandler
	ProblemDetail onConstraintViolation(ConstraintViolationException ex) {
		String detail = ex.getConstraintViolations()
			.stream()
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

}
