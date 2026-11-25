package me.project.joonggoapi.presentation;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.dto.CouponIssueRequest;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssuePolicyCache;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/api/v2/coupon/issue")
@RestController
public class CouponIssueQueueController {

    private final CouponIssuePolicyCache policyCache;
    private final CouponIssueQueue queue;

    @PostMapping
    public ResponseEntity<IssueResult> issue(@RequestBody CouponIssueRequest request) {
        if (request.couponId() == null || request.couponId() <= 0
                || request.memberId() == null || request.memberId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId and memberId must be positive");
        }

        UUID requestId = UUID.randomUUID();
        LocalDateTime requestedAt = LocalDateTime.now();
        policyCache.validate(request.couponId(), requestedAt);
        queue.enqueue(new CouponIssueJob(requestId, request.couponId(), request.memberId(), requestedAt));
        return ResponseEntity.accepted()
                .location(URI.create("/api/v2/coupon/issue/" + requestId))
                .body(new IssueResult(requestId, "PENDING", null));
    }

    @GetMapping("/{requestId}")
    public IssueResult status(@PathVariable UUID requestId) {
        String result = queue.status(requestId);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (result.equals("PENDING") || result.equals("ISSUED")) {
            return new IssueResult(requestId, result, null);
        }
        return new IssueResult(requestId, "REJECTED", result);
    }

    public record IssueResult(UUID requestId, String status, String errorCode) {
    }
}
