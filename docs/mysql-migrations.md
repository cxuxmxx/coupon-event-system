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
