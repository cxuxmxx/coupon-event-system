package me.project.joonggocore.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DUPLICATE_MEMBER(HttpStatus.BAD_REQUEST, "이미 존재하는 유저 입니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    NOT_FOUND_EXERCISE(HttpStatus.NOT_FOUND, "존재하지 않는 운동 기록 입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "패스워드가 잘못되었습니다."),
    NOT_FOUND_TRADE(HttpStatus.NOT_FOUND, "존재하지 않는 거래입니다."),
    INVALID_MEMBER(HttpStatus.UNAUTHORIZED, "접근할 수 없는 거래 입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    INVALID_COUPON_QUANTITY(HttpStatus.BAD_REQUEST, "발급 가능한 수량을 초과합니다."),
    INVALID_COUPON_ISSUE_DATE(HttpStatus.BAD_REQUEST, "발급 가능한 일자가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
