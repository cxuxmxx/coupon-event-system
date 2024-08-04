package me.project.joonggoapi.presentation;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.domain.application.CouponIssueRequestService;
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
    public boolean issue(@RequestBody CouponIssueRequest request) {
        couponIssueRequestService.issueRequestV1(request);
        return true;
    }
}
