ALTER TABLE coupon_issue
    MODIFY coupon_id BIGINT NOT NULL,
    MODIFY member_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_coupon_issue_coupon_member UNIQUE (coupon_id, member_id);
