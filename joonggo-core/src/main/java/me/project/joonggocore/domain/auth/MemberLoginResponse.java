package me.project.joonggocore.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginResponse {

    private String accessToken;

    private String refreshToken;


    public static MemberLoginResponse of(String accessToken, String refreshToken) {
        return new MemberLoginResponse(accessToken, refreshToken);
    }
}
