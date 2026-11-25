package me.project.joonggocore.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DUPLICATE_MEMBER("이미 존재하는 유저 입니다."),
    NOT_FOUND_MEMBER("존재하지 않는 유저입니다."),
    NOT_FOUND_EXERCISE("존재하지 않는 운동 기록 입니다."),
    INVALID_PASSWORD("패스워드가 잘못되었습니다."),
    NOT_FOUND_TRADE("존재하지 않는 거래입니다."),
    INVALID_MEMBER("접근할 수 없는 거래 입니다."),
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    INVALID_COUPON_QUANTITY("발급 가능한 수량을 초과합니다."),
    INVALID_COUPON_ISSUE_DATE("발급 가능한 일자가 아닙니다."),
    NOT_FOUND_COUPON("존재하지 않은 쿠폰입니다."),
    ALREADY_ISSUED_USER("이미 발급받은 유저입니다.");

    private final String message;
}
