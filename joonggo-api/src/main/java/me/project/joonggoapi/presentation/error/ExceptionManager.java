package me.project.joonggoapi.presentation.error;

import me.project.joonggocore.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionManager {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<?> appExcetionManager(AppException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getMessage());
        HttpStatus status = switch (e.getErrorCode()) {
            case NOT_FOUND_MEMBER, NOT_FOUND_EXERCISE, NOT_FOUND_TRADE -> HttpStatus.NOT_FOUND;
            case INVALID_PASSWORD, INVALID_MEMBER -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error("ERROR", errorResponse));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValidExceptionManager(MethodArgumentNotValidException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error("ERROR", extractErrorMessage(e)));
    }

    private List<ErrorResponse> extractErrorMessage(MethodArgumentNotValidException e){
        return e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .map(ErrorResponse::new)
                .collect(Collectors.toList());
    }



}
