package me.project.joonggocore.domain.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberUpdateRequest {

    private String name;

    private String email;

    private String sex;

    private int age;

    private double height;

    private double weight;

}
