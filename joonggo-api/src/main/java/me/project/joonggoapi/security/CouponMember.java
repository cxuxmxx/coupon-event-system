package me.project.joonggoapi.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class CouponMember {

    private CouponMember() {
    }

    public static long from(Jwt jwt, Long requestedMemberId) {
        long memberId;
        try {
            memberId = Long.parseLong(jwt == null ? null : jwt.getSubject());
            if (memberId <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid member subject");
        }
        if (requestedMemberId != null && requestedMemberId != memberId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return memberId;
    }
}
