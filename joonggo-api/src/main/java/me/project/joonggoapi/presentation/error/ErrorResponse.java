package me.project.joonggoapi.presentation.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.project.joonggocore.exception.ErrorCode;

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
