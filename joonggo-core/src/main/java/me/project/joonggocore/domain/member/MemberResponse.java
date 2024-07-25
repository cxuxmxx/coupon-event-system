package me.project.joonggocore.domain.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    public Long id;

    private String email;

    private double height;

    private double weight;

    private String name;

    private String sex;

    private int age;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getNickname())
                .sex(member.getSex())
                .age(member.getAge())
                .build();
    }
}
