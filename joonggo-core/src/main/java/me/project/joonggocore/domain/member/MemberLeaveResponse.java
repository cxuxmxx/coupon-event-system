package me.project.joonggocore.domain.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MemberLeaveResponse {

    private Long id;

    public static MemberLeaveResponse from(Member member) {
        return new MemberLeaveResponse(member.getId());
    }
}
