package me.project.joonggocore.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse<T> {

    private String resultCode;
    private T result;

    public static<T> ApiResponse<T> error(String resultCode, T result){
        return new ApiResponse<>(resultCode, result);
    }

    public static<T> ApiResponse<T> success(T result){
        return new ApiResponse<>("SUCCESS", result);
    }
}
