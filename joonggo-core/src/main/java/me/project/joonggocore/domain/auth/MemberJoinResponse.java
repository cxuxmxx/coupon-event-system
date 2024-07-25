package me.project.joonggocore.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.project.joonggocore.domain.member.Member;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MemberJoinResponse {
    private String name;


    public static MemberJoinResponse from(Member member) {
        return new MemberJoinResponse(member.getNickname());
    }
}
