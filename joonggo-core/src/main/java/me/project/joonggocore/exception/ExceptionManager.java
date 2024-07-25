package me.project.joonggocore.exception;

import me.project.joonggocore.util.ApiResponse;
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
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error("ERROR", errorResponse));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValidExceptionManager(MethodArgumentNotValidException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
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
