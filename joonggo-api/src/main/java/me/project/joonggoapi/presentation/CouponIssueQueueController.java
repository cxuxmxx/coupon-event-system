package me.project.joonggoapi.presentation;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.security.CouponMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import me.project.joonggoapi.application.AsyncCouponIssueService;
import me.project.joonggoapi.application.AsyncCouponIssueService.IdempotencyConflictException;
import me.project.joonggoapi.application.AsyncCouponIssueService.Result;
import me.project.joonggoapi.dto.CouponIssueRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@RequestMapping("/api/v2/coupon/issue")
@RestController
public class CouponIssueQueueController {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private final AsyncCouponIssueService issueService;

    @PostMapping
    public ResponseEntity<Result> issue(@RequestBody CouponIssueRequest request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                        @AuthenticationPrincipal Jwt jwt) {
        long memberId = CouponMember.from(jwt, request.memberId());
        if (request.couponId() == null || request.couponId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId must be positive");
        }

        if (key != null && !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        }

        Result result;
        try {
            result = issueService.submit(request.couponId(), memberId, key);
        } catch (IdempotencyConflictException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency-Key is already used with a different request", e);
        }
        return ResponseEntity.accepted()
                .location(URI.create("/api/v2/coupon/issue/" + result.requestId()))
                .body(result);
    }

    @GetMapping("/{requestId}")
    public Result status(@PathVariable UUID requestId, @AuthenticationPrincipal Jwt jwt) {
        return issueService.findResult(requestId, CouponMember.from(jwt, null))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
