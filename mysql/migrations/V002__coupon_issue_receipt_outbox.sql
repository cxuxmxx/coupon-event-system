CREATE TABLE coupon_issue_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(36) NOT NULL,
    coupon_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    key_hash VARCHAR(64),
    result VARCHAR(64),
    completed_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_receipt_request (request_id),
    UNIQUE KEY uk_coupon_receipt_idempotency (key_hash),
    KEY idx_coupon_receipt_outbox (completed_at, id)
) ENGINE = InnoDB;
