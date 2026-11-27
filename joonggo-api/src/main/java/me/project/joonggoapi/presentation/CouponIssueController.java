package me.project.joonggoapi.presentation;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.security.CouponMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import me.project.joonggoapi.application.CouponIssueRequestService;
import me.project.joonggoapi.dto.CouponIssueRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/coupon")
@RestController
public class CouponIssueController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PostMapping("/issue")
    public boolean issue(@RequestBody CouponIssueRequest request, @AuthenticationPrincipal Jwt jwt) {
        long memberId = CouponMember.from(jwt, request.memberId());
        couponIssueRequestService.issueRequestV1(new CouponIssueRequest(request.couponId(), memberId));
        return true;
    }
}
