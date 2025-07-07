package io.gaboja9.mockstock.domain.members.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponseDto<T> {

    private MemberInfoDto memberInfoDto;

    private T data;
}
