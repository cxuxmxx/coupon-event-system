package me.project.joonggocore.domain.member;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.project.joonggocore.domain.auth.MemberJoinRequest;
import me.project.joonggocore.util.BaseEntity;
import org.hibernate.annotations.Where;


@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Where(clause = "deleted_at is null")
@Entity
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nickname;

    private String password;

    private String email;

    private String sex;

    @Column(unique = true)
    private int age;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(length = 1000)
    private String refreshToken;

    public static Member createMember(
            MemberJoinRequest request,
            String password,
            String refreshToken
    ) {
        return Member.builder()
                .nickname(request.getNickName())
                .password(password)
                .email(request.getEmail())
                .sex(request.getSex())
                .age(request.getAge())
                .role(MemberRole.NORMAL)
                .refreshToken(refreshToken)
                .build();
    }

    public void changeMemberInfo(MemberUpdateRequest request) {
        this.nickname = request.getName();
        this.email = request.getEmail();
        this.sex = request.getSex();
        this.age = request.getAge();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void deleteRefreshToken() {
        this.refreshToken = null;
    }

}
