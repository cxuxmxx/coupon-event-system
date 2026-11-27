# MySQL 스키마 변경

기존 스키마의 변경은 `mysql/migrations`에 번호를 붙여 관리합니다. 적용 전에 백업을 준비하고 쓰기 작업을 멈춥니다. 새 로컬 DB는 Hibernate로 초기 스키마를 만들 수 있습니다.

## V001 · 회원별 쿠폰 1회 발급

적용 전 아래 조회 결과가 비어 있어야 합니다.

```sql
SELECT coupon_id, member_id, COUNT(*) AS issue_count
FROM coupon_issue
GROUP BY coupon_id, member_id
HAVING COUNT(*) > 1 OR coupon_id IS NULL OR member_id IS NULL;
```

결과가 있으면 발급 이력과 쿠폰의 `issued_quantity`를 대조해 정리합니다. 마이그레이션은 기존 이력을 삭제하지 않으며, 제약을 위반한 데이터가 남으면 실패합니다.

```bash
docker compose exec -T mysql mysql -ujoonggo_admin -p1234 joonggo \
  < mysql/migrations/V001__unique_coupon_issue.sql
```

## V002 · 발급 접수 원장과 Outbox

`V002__coupon_issue_receipt_outbox.sql`로 원장 테이블을 만듭니다. 발급 완료 전까지 같은 행을 Outbox로 사용합니다. 접수를 MySQL에 커밋한 뒤 HTTP 202를 반환하고, 워커가 미완료 행을 Redis에 넣습니다. Redis에 장애가 발생하거나 큐를 유실해도 남은 행을 다시 발행합니다. 발급 이력과 요청의 성공 상태는 같은 트랜잭션에서 커밋합니다.

기존 버전의 Redis 대기열은 업데이트 전에 소진하세요. 그렇지 않으면 새 원장에 기록된 요청만 Redis 전체 유실에서 복구할 수 있습니다. 최근 완료 결과는 24시간 동안 조회할 수 있으며, 원장 행 자체는 보존됩니다.

## MySQL 통합 테스트 실행

테스트가 스키마를 생성하고 종료할 때 삭제합니다. 테스트 전용 빈 스키마를 준비하고 다음 명령을 실행합니다.

```bash
COUPON_TEST_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/coupon_test \
COUPON_TEST_MYSQL_USER=coupon_test \
COUPON_TEST_MYSQL_PASSWORD=비밀번호 \
bash gradlew :joonggo-consumer:test --tests '*CouponIssueMysqlIntegrationTest'
```
