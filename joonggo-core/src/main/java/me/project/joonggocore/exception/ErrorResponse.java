package me.project.joonggocore.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ErrorResponse {

    private ErrorCode errorCode;
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
