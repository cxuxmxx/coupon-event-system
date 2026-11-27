# 쿠폰 발급 API 인증

v1과 v2 발급 API, v2 결과 조회에 `Authorization: Bearer <JWT>`가 필요합니다. 배포 환경에는 발급자, 대상 서비스, 공개키 주소를 설정합니다.

| 환경 변수 | 값 |
| --- | --- |
| `COUPON_SECURITY_ISSUER` | 신뢰할 발급자의 JWT `iss` 값 |
| `COUPON_SECURITY_AUDIENCE` | 쿠폰 API의 JWT `aud` 값 |
| `COUPON_SECURITY_JWKS` | 발급자의 JWK Set 주소 |

쿠폰 시스템이 관리하는 양의 정수 회원 ID를 JWT의 `sub`에 담습니다. 요청 본문에서 `memberId`는 생략할 수 있습니다. 예전 클라이언트가 값을 보내면 인증된 회원 ID와 일치해야 합니다. 자기 요청에 대해서만 결과를 볼 수 있으며, 다른 회원의 요청 ID에는 404를 반환합니다.

멱등키는 회원별로 구분합니다. 이전에 원장에 기록한 키는 해당 회원이 재시도할 때 기존 요청을 찾을 수 있습니다. 이전 버전에서 Redis에만 남긴 결과는 회원 소유권을 확인할 수 없어 인증 적용 후에는 조회할 수 없습니다.
