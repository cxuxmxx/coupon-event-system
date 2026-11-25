package me.project.joonggoapi.presentation;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.application.AsyncCouponIssueService;
import me.project.joonggoapi.application.AsyncCouponIssueService.Result;
import me.project.joonggoapi.dto.CouponIssueRequest;
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
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/api/v2/coupon/issue")
@RestController
public class CouponIssueQueueController {

    private final AsyncCouponIssueService issueService;

    @PostMapping
    public ResponseEntity<Result> issue(@RequestBody CouponIssueRequest request) {
        if (request.couponId() == null || request.couponId() <= 0
                || request.memberId() == null || request.memberId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId and memberId must be positive");
        }

        Result result = issueService.submit(request.couponId(), request.memberId());
        return ResponseEntity.accepted()
                .location(URI.create("/api/v2/coupon/issue/" + result.requestId()))
                .body(result);
    }

    @GetMapping("/{requestId}")
    public Result status(@PathVariable UUID requestId) {
        return issueService.findResult(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
