package fr.quinquennat.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.quinquennat.backend.engine.EngineUnavailableException;
import fr.quinquennat.backend.simulation.InvalidProgramException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidProgramException.class)
    public ProblemDetail invalidProgram(InvalidProgramException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EngineUnavailableException.class)
    public ProblemDetail engineUnavailable(EngineUnavailableException e) {
        log.error("Engine call failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Le moteur de calcul est momentanément indisponible. Réessayez dans un instant.");
    }
}
