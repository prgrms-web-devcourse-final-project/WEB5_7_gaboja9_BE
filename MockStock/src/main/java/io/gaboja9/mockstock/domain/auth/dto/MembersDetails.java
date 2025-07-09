package io.gaboja9.mockstock.domain.auth.dto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MembersDetails implements OAuth2User {

	private final Long memberId;
	private String name;
	private Map<String, Object> attributes;

	@Setter
	private String role;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
	}

	@Builder
	public MembersDetails(Long memberId, String name, Map<String, Object> attributes, String role) {
		this.memberId = memberId;
		this.name = name;
		this.attributes = attributes;
		this.role = role;
	}
}
