package me.project.joonggoapi.domain.application;

import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.member.*;
import me.project.joonggocore.domain.member.repository.MemberRepository;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponse find(Long id) {
        return MemberResponse.from(findMemberById(id));
    }

    @Transactional
    public MemberLeaveResponse leave(Long id, String name) {
        Member member = findMemberById(id);
        member.leave();
        return MemberLeaveResponse.from(member);
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.NOT_FOUND_MEMBER,
                        ErrorCode.NOT_FOUND_MEMBER.getMessage()));
    }

    @Transactional
    public MemberUpdateResponse update(Long id, MemberUpdateRequest request) {
        Member member = findMemberById(id);
        member.changeMemberInfo(request);
        return new MemberUpdateResponse(member.getId());
    }

}
