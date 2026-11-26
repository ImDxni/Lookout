package me.dani.lookout.shared.handlers;

import me.dani.lookout.shared.utils.InvalidOriginException;
import me.dani.lookout.site.exceptions.DuplicateSiteException;
import me.dani.lookout.site.exceptions.SiteNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler {
    private static final String SITE_NAME_UNIQUE = "uq_site_name_ci";

    @ExceptionHandler(SiteNotFoundException.class)
    ProblemDetail notFound(SiteNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidOriginException.class)
    ProblemDetail invalidOrigin(InvalidOriginException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSiteException.class)
    ProblemDetail duplicateSite(DuplicateSiteException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,ex.getMessage());
    }

    //Handling concurrent create requests with same name
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException ex) {
        String constraint = violatedConstraint(ex);
        if (SITE_NAME_UNIQUE.equalsIgnoreCase(constraint)) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A site with this name already exists");
        }

        log.error("Unexpected data integrity violation (constraint: {})", constraint, ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authError(AuthenticationException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,"Wrong credentials");
    }


    private static String violatedConstraint(Throwable ex) {

        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
        }
        return null;
    }
}