package me.project.joonggocore.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberJoinRequest {

    private String nickName;

    private String password;

    private String email;

    private String sex;

    private int age;

}
